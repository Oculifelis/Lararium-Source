package com.oculifelis.lararium.compat;
import com.oculifelis.lararium.Lararium;
import mezz.jei.api.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
/** The optional viewer connection; recipe definitions are supplied by Fundamentum. */
@JeiPlugin
public final class LarariumJeiAccess implements IModPlugin {
    public static volatile IJeiRuntime runtime;
    public static boolean filterLararium(){if(runtime==null)return false;runtime.getIngredientFilter().setFilterText("@lararium");return true;}
    public static boolean hasWeaving(){return runtime!=null&&runtime.getRecipeManager().getRecipeType(Lararium.id("weaving")).isPresent();}
    public static boolean hasWeavingRecipe(){return runtime!=null&&runtime.getRecipeManager().getRecipeType(Lararium.id("weaving")).map(t->runtime.getRecipeManager().createRecipeLookup(t).get().findAny().isPresent()).orElse(false);}
    public static void showWeaving(){if(runtime!=null)runtime.getRecipeManager().getRecipeType(Lararium.id("weaving")).ifPresent(t->runtime.getRecipesGui().showTypes(java.util.List.of(t)));}
    @Override public Identifier getPluginUid(){return Lararium.id("recipes");}
    @Override public void onRuntimeAvailable(IJeiRuntime r){runtime=r;}
    @Override public void onRuntimeUnavailable(){runtime=null;}
}
