package com.oculifelis.lararium;

import com.fundamentum.recipeview.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;

public final class LarariumViews {
    public static void init(){
        ViewCategory<LarariumData.Weave> c=RecipeViews.category("lararium:weaving");
        c.title(Component.translatable("jei.lararium.weaving")).icon(()->new ItemStack(Lararium.HEART_ITEM.get())).size(152,58)
            .arrow(64,7,80).catalysts(()->List.of(new ItemStack(Lararium.HEART_ITEM.get())));
        int input=c.input(28,6),output=c.output(108,6);
        c.recipes(()->List.copyOf(LarariumData.WEAVES.entries().values())).key(r->r.input().toString().replace(':','/')).fill((r,page)->{
            page.item(input,new ItemStack(BuiltInRegistries.ITEM.getValue(r.input()))).item(output,r.result());
            page.text(Component.translatable("jei.lararium.cost",r.breaths(),r.ticks()/20.),35);
            page.text(Component.translatable("jei.lararium.awake"),48);
        });
        RecipeViews.info(()->List.of(new ItemStack(Lararium.HEART_ITEM.get())),()->List.of(Component.translatable("jei.lararium.heart")));
        RecipeViews.info(()->List.of(new ItemStack(Lararium.HOOK.get())),()->List.of(Component.translatable("jei.lararium.hook")));
        RecipeViews.info(()->List.of(new ItemStack(Lararium.BROOCH.get())),()->List.of(Component.translatable("jei.lararium.brooch")));
        RecipeViews.info(()->List.of(new ItemStack(Lararium.THREAD.get())),()->List.of(Component.translatable("jei.lararium.answer")));
        RecipeViews.info(()->List.of(new ItemStack(Lararium.FORK.get())),()->List.of(Component.translatable("jei.lararium.keeper")));
    }
    private LarariumViews(){}
}
