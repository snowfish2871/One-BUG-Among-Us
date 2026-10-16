package net.minecraft.world.item.component;

import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Stream;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public final class ItemContainerContents {
    private static final int NO_SLOT = -1;
    private static final int MAX_SIZE = 256;
    public static final ItemContainerContents EMPTY = new ItemContainerContents(NonNullList.create());
    public static final Codec<ItemContainerContents> CODEC = ItemContainerContents.Slot.CODEC
        .sizeLimitedListOf(256)
        .xmap(ItemContainerContents::fromSlots, ItemContainerContents::asSlots);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemContainerContents> STREAM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC
        .apply(ByteBufCodecs.list(256))
        .map(ItemContainerContents::new, p_331691_ -> p_331691_.items);
    private final NonNullList<ItemStack> items;
    private final int hashCode;

    private ItemContainerContents(NonNullList<ItemStack> p_332193_) {
        if (p_332193_.size() > 256) {
            throw new IllegalArgumentException("Got " + p_332193_.size() + " items, but maximum is 256");
        } else {
            this.items = p_332193_;
            this.hashCode = ItemStack.hashStackList(p_332193_);
        }
    }

    private ItemContainerContents(int p_331689_) {
        this(NonNullList.withSize(p_331689_, ItemStack.EMPTY));
    }

    private ItemContainerContents(List<ItemStack> p_331046_) {
        this(p_331046_.size());

        for (int i = 0; i < p_331046_.size(); i++) {
            this.items.set(i, p_331046_.get(i));
        }
    }

    private static ItemContainerContents fromSlots(List<ItemContainerContents.Slot> p_331424_) {
        OptionalInt optionalint = p_331424_.stream().mapToInt(ItemContainerContents.Slot::index).max();
        if (optionalint.isEmpty()) {
            return EMPTY;
        } else {
            ItemContainerContents itemcontainercontents = new ItemContainerContents(optionalint.getAsInt() + 1);

            for (ItemContainerContents.Slot itemcontainercontents$slot : p_331424_) {
                itemcontainercontents.items.set(itemcontainercontents$slot.index(), itemcontainercontents$slot.item());
            }

            return itemcontainercontents;
        }
    }

    public static ItemContainerContents fromItems(List<ItemStack> p_340879_) {
        int i = findLastNonEmptySlot(p_340879_);
        if (i == -1) {
            return EMPTY;
        } else {
            ItemContainerContents itemcontainercontents = new ItemContainerContents(i + 1);

            for (int j = 0; j <= i; j++) {
                itemcontainercontents.items.set(j, p_340879_.get(j).copy());
            }

            return itemcontainercontents;
        }
    }

    private static int findLastNonEmptySlot(List<ItemStack> p_340916_) {
        for (int i = p_340916_.size() - 1; i >= 0; i--) {
            if (!p_340916_.get(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    private List<ItemContainerContents.Slot> asSlots() {
        List<ItemContainerContents.Slot> list = new ArrayList<>();

        for (int i = 0; i < this.items.size(); i++) {
            ItemStack itemstack = this.items.get(i);
            if (!itemstack.isEmpty()) {
                list.add(new ItemContainerContents.Slot(i, itemstack));
            }
        }

        return list;
    }

    public void copyInto(NonNullList<ItemStack> p_330513_) {
        for (int i = 0; i < p_330513_.size(); i++) {
            ItemStack itemstack = i < this.items.size() ? this.items.get(i) : ItemStack.EMPTY;
            p_330513_.set(i, itemstack.copy());
        }
    }

    public ItemStack copyOne() {
        return this.items.isEmpty() ? ItemStack.EMPTY : this.items.get(0).copy();
    }

    public Stream<ItemStack> stream() {
        return this.items.stream().map(ItemStack::copy);
    }

    public Stream<ItemStack> nonEmptyStream() {
        return this.items.stream().filter(p_331322_ -> !p_331322_.isEmpty()).map(ItemStack::copy);
    }

    public Iterable<ItemStack> nonEmptyItems() {
        return Iterables.filter(this.items, p_331420_ -> !p_331420_.isEmpty());
    }

    public Iterable<ItemStack> nonEmptyItemsCopy() {
        return Iterables.transform(this.nonEmptyItems(), ItemStack::copy);
    }

    @Override
    public boolean equals(Object p_331711_) {
        if (this == p_331711_) {
            return true;
        } else {
            if (p_331711_ instanceof ItemContainerContents itemcontainercontents && ItemStack.listMatches(this.items, itemcontainercontents.items)) {
                return true;
            }

            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    static record Slot(int index, ItemStack item) {
        public static final Codec<ItemContainerContents.Slot> CODEC = RecordCodecBuilder.create(
            p_331695_ -> p_331695_.group(
                        Codec.intRange(0, 255).fieldOf("slot").forGetter(ItemContainerContents.Slot::index),
                        ItemStack.CODEC.fieldOf("item").forGetter(ItemContainerContents.Slot::item)
                    )
                    .apply(p_331695_, ItemContainerContents.Slot::new)
        );
    }
}
