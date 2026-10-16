package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import org.apache.commons.lang3.mutable.MutableObject;

public class FunctionCommand {
   private static final DynamicCommandExceptionType ERROR_ARGUMENT_NOT_COMPOUND = new DynamicCommandExceptionType(
      p_293774_ -> Component.translatable("commands.function.error.argument_not_compound", p_293774_)
   );
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_FUNCTION = (p_137719_, p_137720_) -> {
      ServerFunctionManager serverfunctionmanager = p_137719_.getSource().getServer().getFunctions();
      SharedSuggestionProvider.suggestResource(serverfunctionmanager.getTagNames(), p_137720_, "#");
      return SharedSuggestionProvider.suggestResource(serverfunctionmanager.getFunctionNames(), p_137720_);
   };

   public static void register(CommandDispatcher<CommandSourceStack> p_137715_) {
      LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = Commands.literal("with");

      for(DataCommands.DataProvider datacommands$dataprovider : DataCommands.SOURCE_PROVIDERS) {
         datacommands$dataprovider.wrap(
            literalargumentbuilder,
            p_293771_ -> p_293771_.executes(
                     p_293773_ -> runFunction(
                           p_293773_.getSource(), FunctionArgument.getFunctions(p_293773_, "name"), datacommands$dataprovider.access(p_293773_).getData()
                        )
                  )
                  .then(
                     Commands.argument("path", NbtPathArgument.nbtPath())
                        .executes(
                           p_293776_ -> runFunction(
                                 p_293776_.getSource(),
                                 FunctionArgument.getFunctions(p_293776_, "name"),
                                 getArgumentTag(NbtPathArgument.getPath(p_293776_, "path"), datacommands$dataprovider.access(p_293776_))
                              )
                        )
                  )
         );
      }

      p_137715_.register(
         Commands.literal("function")
            .requires(p_137722_ -> p_137722_.hasPermission(2))
            .then(
               Commands.argument("name", FunctionArgument.functions())
                  .suggests(SUGGEST_FUNCTION)
                  .executes(p_293769_ -> runFunction(p_293769_.getSource(), FunctionArgument.getFunctions(p_293769_, "name"), null))
                  .then(
                     Commands.argument("arguments", CompoundTagArgument.compoundTag())
                        .executes(
                           p_293777_ -> runFunction(
                                 p_293777_.getSource(),
                                 FunctionArgument.getFunctions(p_293777_, "name"),
                                 CompoundTagArgument.getCompoundTag(p_293777_, "arguments")
                              )
                        )
                  )
                  .then(literalargumentbuilder)
            )
      );
   }

   private static CompoundTag getArgumentTag(NbtPathArgument.NbtPath p_295114_, DataAccessor p_296132_) throws CommandSyntaxException {
      Tag tag = DataCommands.getSingleTag(p_295114_, p_296132_);
      if (tag instanceof CompoundTag) {
         return (CompoundTag)tag;
      } else {
         throw ERROR_ARGUMENT_NOT_COMPOUND.create(tag.getType().getName());
      }
   }

   private static int runFunction(CommandSourceStack p_296484_, Collection<CommandFunction> p_296332_, @Nullable CompoundTag p_295499_) {
      int i = 0;
      boolean flag = false;
      boolean flag1 = false;

      for(CommandFunction commandfunction : p_296332_) {
         try {
            FunctionCommand.FunctionResult functioncommand$functionresult = runFunction(p_296484_, commandfunction, p_295499_);
            i += functioncommand$functionresult.value();
            flag |= functioncommand$functionresult.isReturn();
            flag1 = true;
         } catch (FunctionInstantiationException functioninstantiationexception) {
            p_296484_.sendFailure(functioninstantiationexception.messageComponent());
         }
      }

      if (flag1) {
         int j = i;
         if (p_296332_.size() == 1) {
            if (flag) {
               p_296484_.sendSuccess(() -> Component.translatable("commands.function.success.single.result", j, p_296332_.iterator().next().getId()), true);
            } else {
               p_296484_.sendSuccess(() -> Component.translatable("commands.function.success.single", j, p_296332_.iterator().next().getId()), true);
            }
         } else if (flag) {
            p_296484_.sendSuccess(() -> Component.translatable("commands.function.success.multiple.result", p_296332_.size()), true);
         } else {
            p_296484_.sendSuccess(() -> Component.translatable("commands.function.success.multiple", j, p_296332_.size()), true);
         }
      }

      return i;
   }

   public static FunctionCommand.FunctionResult runFunction(CommandSourceStack p_137724_, CommandFunction p_295814_, @Nullable CompoundTag p_295422_) throws FunctionInstantiationException {
      MutableObject<FunctionCommand.FunctionResult> mutableobject = new MutableObject<>();
      int i = p_137724_.getServer()
         .getFunctions()
         .execute(
            p_295814_,
            p_137724_.withSuppressedOutput()
               .withMaximumPermission(2)
               .withReturnValueConsumer(p_293768_ -> mutableobject.setValue(new FunctionCommand.FunctionResult(p_293768_, true))),
            null,
            p_295422_
         );
      FunctionCommand.FunctionResult functioncommand$functionresult = mutableobject.getValue();
      return functioncommand$functionresult != null ? functioncommand$functionresult : new FunctionCommand.FunctionResult(i, false);
   }

   public static record FunctionResult(int value, boolean isReturn) {
   }
}
