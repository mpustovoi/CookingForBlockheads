package net.blay09.mods.cookingforblockheads.crafting;

import net.blay09.mods.cookingforblockheads.tag.ModItemTags;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record RecipeWithStatus(ResourceLocation recipeId, ItemStack resultItem, List<Ingredient> missingIngredients,
                               int missingIngredientsMask, NonNullList<ItemStack> lockedInputs) {

    public RecipeHolder<?> recipe(Player player) {
        return player.level().getRecipeManager().byKey(recipeId).orElse(null);
    }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(recipeId);
        buf.writeItem(resultItem);
        buf.writeInt(missingIngredientsMask);
        buf.writeInt(missingIngredients.size());
        for (final var ingredient : missingIngredients) {
            ingredient.toNetwork(buf);
        }
        if (lockedInputs != null) {
            buf.writeInt(lockedInputs.size());
            for (final var lockedInput : lockedInputs) {
                buf.writeItem(lockedInput);
            }
        } else {
            buf.writeInt(0);
        }
    }

    public static RecipeWithStatus fromNetwork(FriendlyByteBuf buf) {
        final var recipeId = buf.readResourceLocation();
        final var resultItem = buf.readItem();
        final var missingIngredientsMask = buf.readInt();
        final var missingIngredientCount = buf.readInt();
        final var missingIngredients = new ArrayList<Ingredient>(missingIngredientCount);
        for (int j = 0; j < missingIngredientCount; j++) {
            missingIngredients.add(Ingredient.fromNetwork(buf));
        }
        final var lockedInputCount = buf.readInt();
        final var lockedInputs = NonNullList.withSize(lockedInputCount, ItemStack.EMPTY);
        for (int j = 0; j < lockedInputCount; j++) {
            lockedInputs.set(j, buf.readItem());
        }
        return new RecipeWithStatus(recipeId, resultItem, missingIngredients, missingIngredientsMask, lockedInputs);
    }

    public static RecipeWithStatus best(@Nullable RecipeWithStatus first, @Nullable RecipeWithStatus second) {
        if (first == null) {
            return second;
        } else if (second == null) {
            return first;
        }

        if (first.missingIngredients.size() < second.missingIngredients.size()) {
            return first;
        } else if (second.missingIngredients.size() < first.missingIngredients.size()) {
            return second;
        }

        return first;
    }

    public boolean canCraft() {
        return missingIngredients.isEmpty();
    }

    public boolean isMissingUtensils() {
        return missingIngredients.stream().anyMatch(RecipeWithStatus::isUtensil);
    }

    private static boolean isUtensil(Ingredient ingredient) {
        for (final var itemStack : ingredient.getItems()) {
            if (itemStack.is(ModItemTags.UTENSILS)) {
                return true;
            }
        }

        return false;
    }
}
