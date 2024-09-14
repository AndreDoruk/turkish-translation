package dev.mayaqq.estrogen.datagen.impl.recipes.create;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import dev.mayaqq.estrogen.datagen.base.recipes.BaseRecipeProvider;
import dev.mayaqq.estrogen.datagen.base.platform.recipes.FabricRecipeHelper;
import dev.mayaqq.estrogen.datagen.base.platform.recipes.ForgeRecipeHelper;
import dev.mayaqq.estrogen.datagen.base.platform.recipes.PlatformRecipeHelper;
import dev.mayaqq.estrogen.registry.EstrogenFluids;
import dev.mayaqq.estrogen.registry.EstrogenItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.world.item.Items;

import static dev.mayaqq.estrogen.Estrogen.id;

public class EstrogenEmptyingRecipes extends BaseRecipeProvider {

    public EstrogenEmptyingRecipes(FabricDataOutput output, PlatformRecipeHelper helper) {
        super(output, helper);
    }

    @Override
    protected void init() {
        create(id("horse_urine"), recipeBuilder -> recipeBuilder
                .require(EstrogenItems.HORSE_URINE_BOTTLE.get())
                .output(EstrogenFluids.HORSE_URINE.get(), getAmount(27000))
                .output(Items.GLASS_BOTTLE)
        );
    }

    public static EstrogenEmptyingRecipes buildFabric(FabricDataOutput output) {
        return new EstrogenEmptyingRecipes(output, new FabricRecipeHelper());
    }

    public static EstrogenEmptyingRecipes buildForge(FabricDataOutput output) {
        return new EstrogenEmptyingRecipes(output, new ForgeRecipeHelper());
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return AllRecipeTypes.EMPTYING;
    }
}