package info.mudbourn.mmsweapons.material;

import net.penumbra.enderscape.item.ItemStackContext;
import net.penumbra.enderscape.item.component.FueledTool;
import net.penumbra.enderscape.registry.component.EnderscapeDataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Enderscape Nebulite fuel on shadoline weapons, so their abilities run on fuel like the Dagger and Mirror.
public final class NebuliteFuel {

    static final int MAX_FUEL = 5;

    private NebuliteFuel() {
    }

    static Item.Properties fueled(Item.Properties properties) {
        return properties.component(EnderscapeDataComponents.FUELED_TOOL, FueledTool.simple(MAX_FUEL));
    }

    // Whether the stack has enough fuel for one use; a stack without fuel storage always can.
    public static boolean canSpend(ItemStack stack, LivingEntity user) {
        return !FueledTool.is(stack) || FueledTool.currentFuel(stack) >= FueledTool.fuelCost(new ItemStackContext(stack, user.level(), user));
    }

    // Spends one use of fuel on the server.
    public static void spend(ItemStack stack, LivingEntity user) {
        if (FueledTool.is(stack) && !user.level().isClientSide()) {
            FueledTool.useFuel(new ItemStackContext(stack, user.level(), user));
        }
    }
}
