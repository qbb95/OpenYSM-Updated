package com.elfmcys.yesstevemodel.client.animation.condition;

import net.minecraft.tags.ItemTags;
import rip.ysm.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import rip.ysm.compat.slashblade.SlashBladeCompat;
import com.elfmcys.yesstevemodel.util.ItemTagsConstants;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;

public class InnerClassify {

    private static final String EMPTY = "";

    public static String doClassifyTest(String str, LivingEntity livingEntity, InteractionHand interactionHand) {
        String itemType = getItemType(livingEntity.getItemInHand(interactionHand));
        if (!itemType.equals("")) {
            return str + itemType;
        }
        return "";
    }

    public static String getItemType(ItemStack itemStack) {
        Item item = itemStack.getItem();
        if (SlashBladeCompat.isSlashBladeItem(itemStack)) {
            return "slashblade";
        }
        if (itemStack.is(ItemTagsConstants.SWORDS)) {
            return "sword";
        }
        if (item instanceof MaceItem || itemStack.is(ItemTagsConstants.MACE)) {
            return "mace";
        }
        if (TouhouLittleMaidCompat.isMaidItem(item)) {
            return "gohei";
        }
        if (itemStack.is(ItemTagsConstants.AXES)) {
            return "axe";
        }
        if (itemStack.is(ItemTagsConstants.PICKAXES)) {
            return "pickaxe";
        }
        if (itemStack.is(ItemTagsConstants.SHOVELS)) {
            return "shovel";
        }
        if (itemStack.is(ItemTagsConstants.HOES)) {
            return "hoe";
        }
        if ((item instanceof ShieldItem) || itemStack.is(ItemTagsConstants.SHIELDS)) {
            return "shield";
        }
        if ((item instanceof CrossbowItem) || itemStack.is(ItemTagsConstants.CROSSBOWS)) {
            return "crossbow";
        }
        if ((item instanceof BowItem) || itemStack.is(ItemTagsConstants.BOWS)) {
            return "bow";
        }
        if ((item instanceof FishingRodItem) || itemStack.is(ItemTagsConstants.FISHING_RODS)) {
            return "fishing_rod";
        }
        if ((item instanceof TridentItem) || itemStack.is(ItemTagsConstants.TRIDENTS)) {
            return "spear";
        }
        if (itemStack.is(ItemTagsConstants.PIKE)) {
            return "lance";
        }
        if ((item instanceof ThrowablePotionItem) || itemStack.is(ItemTagsConstants.THROWABLE_POTION)) {
            return "throwable_potion";
        }
        return "";
    }
}