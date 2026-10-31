package net.minecraft.commands;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerFunctionManager;

public class CommandFunction {
   private final CommandFunction.Entry[] entries;
   final ResourceLocation id;

   public CommandFunction(ResourceLocation p_77979_, CommandFunction.Entry[] p_77980_) {
      this.id = p_77979_;
      this.entries = p_77980_;
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public CommandFunction.Entry[] getEntries() {
      return this.entries;
   }

   public CommandFunction instantiate(@Nullable CompoundTag p_294718_, CommandDispatcher<CommandSourceStack> p_294805_, CommandSourceStack p_295042_) throws FunctionInstantiationException {
      return this;
   }

   private static boolean shouldConcatenateNextLine(CharSequence p_294821_) {
      int i = p_294821_.length();
      return i > 0 && p_294821_.charAt(i - 1) == '\\';
   }

   public static CommandFunction fromLines(
      ResourceLocation p_77985_, CommandDispatcher<CommandSourceStack> p_77986_, CommandSourceStack p_77987_, List<String> p_77988_
   ) {
      List<CommandFunction.Entry> list = new ArrayList<>(p_77988_.size());
      Set<String> set = new ObjectArraySet<>();

      for(int i = 0; i < p_77988_.size(); ++i) {
         int j = i + 1;
         String s = p_77988_.get(i).trim();
         String s1;
         if (shouldConcatenateNextLine(s)) {
            StringBuilder stringbuilder = new StringBuilder(s);

            do {
               if (++i == p_77988_.size()) {
                  throw new IllegalArgumentException("Line continuation at end of file");
               }

               stringbuilder.deleteCharAt(stringbuilder.length() - 1);
               String s2 = p_77988_.get(i).trim();
               stringbuilder.append(s2);
            } while(shouldConcatenateNextLine(stringbuilder));

            s1 = stringbuilder.toString();
         } else {
            s1 = s;
         }

         StringReader stringreader = new StringReader(s1);
         if (stringreader.canRead() && stringreader.peek() != '#') {
            if (stringreader.peek() == '/') {
               stringreader.skip();
               if (stringreader.peek() == '/') {
                  throw new IllegalArgumentException(
                     "Unknown or invalid command '" + s1 + "' on line " + j + " (if you intended to make a comment, use '#' not '//')"
                  );
               }

               String s3 = stringreader.readUnquotedString();
               throw new IllegalArgumentException(
                  "Unknown or invalid command '" + s1 + "' on line " + j + " (did you mean '" + s3 + "'? Do not use a preceding forwards slash.)"
               );
            }

            if (stringreader.peek() == '$') {
               CommandFunction.MacroEntry commandfunction$macroentry = decomposeMacro(s1.substring(1), j);
               list.add(commandfunction$macroentry);
               set.addAll(commandfunction$macroentry.parameters());
            } else {
               try {
                  ParseResults<CommandSourceStack> parseresults = p_77986_.parse(stringreader, p_77987_);
                  if (parseresults.getReader().canRead()) {
                     throw Commands.getParseException(parseresults);
                  }

                  list.add(new CommandFunction.CommandEntry(parseresults));
               } catch (CommandSyntaxException commandsyntaxexception) {
                  throw new IllegalArgumentException("Whilst parsing command on line " + j + ": " + commandsyntaxexception.getMessage());
               }
            }
         }
      }

      return (CommandFunction)(set.isEmpty()
         ? new CommandFunction(p_77985_, list.toArray(p_294176_ -> new CommandFunction.Entry[p_294176_]))
         : new CommandFunction.CommandMacro(p_77985_, list.toArray(p_295604_ -> new CommandFunction.Entry[p_295604_]), List.copyOf(set)));
   }

   @VisibleForTesting
   public static CommandFunction.MacroEntry decomposeMacro(String p_294979_, int p_294917_) {
      Builder<String> builder = ImmutableList.builder();
      Builder<String> builder1 = ImmutableList.builder();
      int i = p_294979_.length();
      int j = 0;
      int k = p_294979_.indexOf(36);

      while(k != -1) {
         if (k != i - 1 && p_294979_.charAt(k + 1) == '(') {
            builder.add(p_294979_.substring(j, k));
            int l = p_294979_.indexOf(41, k + 1);
            if (l == -1) {
               throw new IllegalArgumentException("Unterminated macro variable in macro '" + p_294979_ + "' on line " + p_294917_);
            }

            String s = p_294979_.substring(k + 2, l);
            if (!isValidVariableName(s)) {
               throw new IllegalArgumentException("Invalid macro variable name '" + s + "' on line " + p_294917_);
            }

            builder1.add(s);
            j = l + 1;
            k = p_294979_.indexOf(36, j);
         } else {
            k = p_294979_.indexOf(36, k + 1);
         }
      }

      if (j == 0) {
         throw new IllegalArgumentException("Macro without variables on line " + p_294917_);
      } else {
         if (j != i) {
            builder.add(p_294979_.substring(j));
         }

         return new CommandFunction.MacroEntry(builder.build(), builder1.build());
      }
   }

   private static boolean isValidVariableName(String p_295590_) {
      for(int i = 0; i < p_295590_.length(); ++i) {
         char c0 = p_295590_.charAt(i);
         if (!Character.isLetterOrDigit(c0) && c0 != '_') {
            return false;
         }
      }

      return true;
   }

   public static class CacheableFunction {
      public static final CommandFunction.CacheableFunction NONE = new CommandFunction.CacheableFunction((ResourceLocation)null);
      @Nullable
      private final ResourceLocation id;
      private boolean resolved;
      private Optional<CommandFunction> function = Optional.empty();

      public CacheableFunction(@Nullable ResourceLocation p_77998_) {
         this.id = p_77998_;
      }

      public CacheableFunction(CommandFunction p_77996_) {
         this.resolved = true;
         this.id = null;
         this.function = Optional.of(p_77996_);
      }

      public Optional<CommandFunction> get(ServerFunctionManager p_78003_) {
         if (!this.resolved) {
            if (this.id != null) {
               this.function = p_78003_.get(this.id);
            }

            this.resolved = true;
         }

         return this.function;
      }

      @Nullable
      public ResourceLocation getId() {
         return this.function.<ResourceLocation>map(p_78001_ -> p_78001_.id).orElse(this.id);
      }
   }

   public static class CommandEntry implements CommandFunction.Entry {
      private final ParseResults<CommandSourceStack> parse;

      public CommandEntry(ParseResults<CommandSourceStack> p_78006_) {
         this.parse = p_78006_;
      }

      @Override
      public void execute(
         ServerFunctionManager p_164879_,
         CommandSourceStack p_164880_,
         Deque<ServerFunctionManager.QueuedCommand> p_164881_,
         int p_164882_,
         int p_164883_,
         @Nullable ServerFunctionManager.TraceCallbacks p_164884_
      ) throws CommandSyntaxException {
         if (p_164884_ != null) {
            String s = this.parse.getReader().getString();
            p_164884_.onCommand(p_164883_, s);
            int i = this.execute(p_164879_, p_164880_);
            p_164884_.onReturn(p_164883_, s, i);
         } else {
            this.execute(p_164879_, p_164880_);
         }
      }

      private int execute(ServerFunctionManager p_164876_, CommandSourceStack p_164877_) throws CommandSyntaxException {
         return p_164876_.getDispatcher().execute(Commands.mapSource(this.parse, p_242934_ -> p_164877_));
      }

      @Override
      public String toString() {
         return this.parse.getReader().getString();
      }
   }

   static class CommandMacro extends CommandFunction {
      private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#");
      private final List<String> parameters;
      private static final int MAX_CACHE_ENTRIES = 8;
      private final Object2ObjectLinkedOpenHashMap<List<String>, CommandFunction> cache = new Object2ObjectLinkedOpenHashMap<>(8, 0.25F);

      public CommandMacro(ResourceLocation p_296430_, CommandFunction.Entry[] p_294679_, List<String> p_295880_) {
         super(p_296430_, p_294679_);
         this.parameters = p_295880_;
      }

      @Override
      public CommandFunction instantiate(@Nullable CompoundTag p_295120_, CommandDispatcher<CommandSourceStack> p_294437_, CommandSourceStack p_295334_) throws FunctionInstantiationException {
         if (p_295120_ == null) {
            throw new FunctionInstantiationException(Component.translatable("commands.function.error.missing_arguments", this.getId()));
         } else {
            List<String> list = new ArrayList<>(this.parameters.size());

            for(String s : this.parameters) {
               if (!p_295120_.contains(s)) {
                  throw new FunctionInstantiationException(Component.translatable("commands.function.error.missing_argument", this.getId(), s));
               }

               list.add(stringify(p_295120_.get(s)));
            }

            CommandFunction commandfunction = this.cache.getAndMoveToLast(list);
            if (commandfunction != null) {
               return commandfunction;
            } else {
               if (this.cache.size() >= 8) {
                  this.cache.removeFirst();
               }

               CommandFunction commandfunction1 = this.substituteAndParse(list, p_294437_, p_295334_);
               if (commandfunction1 != null) {
                  this.cache.put(list, commandfunction1);
               }

               return commandfunction1;
            }
         }
      }

      private static String stringify(Tag p_302015_) {
         if (p_302015_ instanceof FloatTag floattag) {
            return DECIMAL_FORMAT.format((double)floattag.getAsFloat());
         } else if (p_302015_ instanceof DoubleTag doubletag) {
            return DECIMAL_FORMAT.format(doubletag.getAsDouble());
         } else if (p_302015_ instanceof ByteTag bytetag) {
            return String.valueOf(bytetag.getAsByte());
         } else if (p_302015_ instanceof ShortTag shorttag) {
            return String.valueOf(shorttag.getAsShort());
         } else {
            return p_302015_ instanceof LongTag longtag ? String.valueOf(longtag.getAsLong()) : p_302015_.getAsString();
         }
      }

      private CommandFunction substituteAndParse(List<String> p_295911_, CommandDispatcher<CommandSourceStack> p_294472_, CommandSourceStack p_295841_) throws FunctionInstantiationException {
         CommandFunction.Entry[] acommandfunction$entry = this.getEntries();
         CommandFunction.Entry[] acommandfunction$entry1 = new CommandFunction.Entry[acommandfunction$entry.length];

         for(int i = 0; i < acommandfunction$entry.length; ++i) {
            CommandFunction.Entry commandfunction$entry = acommandfunction$entry[i];
            if (!(commandfunction$entry instanceof CommandFunction.MacroEntry)) {
               acommandfunction$entry1[i] = commandfunction$entry;
            } else {
               CommandFunction.MacroEntry commandfunction$macroentry = (CommandFunction.MacroEntry)commandfunction$entry;
               List<String> list = commandfunction$macroentry.parameters();
               List<String> list1 = new ArrayList<>(list.size());

               for(String s : list) {
                  list1.add(p_295911_.get(this.parameters.indexOf(s)));
               }

               String s1 = commandfunction$macroentry.substitute(list1);

               try {
                  ParseResults<CommandSourceStack> parseresults = p_294472_.parse(s1, p_295841_);
                  if (parseresults.getReader().canRead()) {
                     throw Commands.getParseException(parseresults);
                  }

                  acommandfunction$entry1[i] = new CommandFunction.CommandEntry(parseresults);
               } catch (CommandSyntaxException commandsyntaxexception) {
                  throw new FunctionInstantiationException(
                     Component.translatable("commands.function.error.parse", this.getId(), s1, commandsyntaxexception.getMessage())
                  );
               }
            }
         }

         ResourceLocation resourcelocation = this.getId();
         return new CommandFunction(
            new ResourceLocation(resourcelocation.getNamespace(), resourcelocation.getPath() + "/" + p_295911_.hashCode()), acommandfunction$entry1
         );
      }

      static {
         DECIMAL_FORMAT.setMaximumFractionDigits(15);
         DECIMAL_FORMAT.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
      }
   }

   @FunctionalInterface
   public interface Entry {
      void execute(
         ServerFunctionManager p_164885_,
         CommandSourceStack p_164886_,
         Deque<ServerFunctionManager.QueuedCommand> p_164887_,
         int p_164888_,
         int p_164889_,
         @Nullable ServerFunctionManager.TraceCallbacks p_164890_
      ) throws CommandSyntaxException;
   }

   public static class FunctionEntry implements CommandFunction.Entry {
      private final CommandFunction.CacheableFunction function;

      public FunctionEntry(CommandFunction p_78019_) {
         this.function = new CommandFunction.CacheableFunction(p_78019_);
      }

      @Override
      public void execute(
         ServerFunctionManager p_164902_,
         CommandSourceStack p_164903_,
         Deque<ServerFunctionManager.QueuedCommand> p_164904_,
         int p_164905_,
         int p_164906_,
         @Nullable ServerFunctionManager.TraceCallbacks p_164907_
      ) {
         Util.ifElse(this.function.get(p_164902_), p_164900_ -> {
            CommandFunction.Entry[] acommandfunction$entry = p_164900_.getEntries();
            if (p_164907_ != null) {
               p_164907_.onCall(p_164906_, p_164900_.getId(), acommandfunction$entry.length);
            }

            int i = p_164905_ - p_164904_.size();
            int j = Math.min(acommandfunction$entry.length, i);

            for(int k = j - 1; k >= 0; --k) {
               p_164904_.addFirst(new ServerFunctionManager.QueuedCommand(p_164903_, p_164906_ + 1, acommandfunction$entry[k]));
            }
         }, () -> {
            if (p_164907_ != null) {
               p_164907_.onCall(p_164906_, this.function.getId(), -1);
            }
         });
      }

      @Override
      public String toString() {
         return "function " + this.function.getId();
      }
   }

   public static class MacroEntry implements CommandFunction.Entry {
      private final List<String> segments;
      private final List<String> parameters;

      public MacroEntry(List<String> p_296312_, List<String> p_294534_) {
         this.segments = p_296312_;
         this.parameters = p_294534_;
      }

      public List<String> parameters() {
         return this.parameters;
      }

      public String substitute(List<String> p_294418_) {
         StringBuilder stringbuilder = new StringBuilder();

         for(int i = 0; i < this.parameters.size(); ++i) {
            stringbuilder.append(this.segments.get(i)).append(p_294418_.get(i));
         }

         if (this.segments.size() > this.parameters.size()) {
            stringbuilder.append(this.segments.get(this.segments.size() - 1));
         }

         return stringbuilder.toString();
      }

      @Override
      public void execute(
         ServerFunctionManager p_296313_,
         CommandSourceStack p_296098_,
         Deque<ServerFunctionManager.QueuedCommand> p_294982_,
         int p_295446_,
         int p_295497_,
         @Nullable ServerFunctionManager.TraceCallbacks p_294623_
      ) throws CommandSyntaxException {
         throw new IllegalStateException("Tried to execute an uninstantiated macro");
      }
   }
}
