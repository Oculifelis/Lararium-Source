package com.oculifelis.lararium;

import com.fundamentum.data.DataTable;
import com.fundamentum.data.DataTables;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class LarariumData {
    public static final DataTable<JsonObject> RULES=DataTables.registerJson(Lararium.id("rules"),"lararium/rules").synced();
    public static final DataTable<Weave> WEAVES=DataTables.register(Lararium.id("weaving"),"lararium/weaving",Weave::read).synced().onLoad(t->com.fundamentum.recipeview.RecipeViews.refresh());
    public record Weave(Identifier input,Identifier output,int count,int breaths,int ticks) {
        static Weave read(JsonObject j) {
            return new Weave(Identifier.parse(j.get("input").getAsString()),Identifier.parse(j.get("output").getAsString()),
                clamp(j,"count",1,1,64),clamp(j,"breaths",2,1,256),clamp(j,"ticks",80,20,72000));
        }
        public boolean matches(ItemStack s) { return !s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).equals(input); }
        public ItemStack result() { return new ItemStack(BuiltInRegistries.ITEM.getValue(output),count); }
    }
    private static int clamp(JsonObject j,String k,int fallback,int min,int max) { return Math.clamp(j.has(k)?j.get(k).getAsInt():fallback,min,max); }
    public static int rule(String k,int fallback,int min,int max) { return Math.clamp((int)RULES.number(Lararium.id("default"),k,fallback),min,max); }
    public static double decimal(String k,double fallback,double min,double max){double v=RULES.number(Lararium.id("default"),k,fallback);return Double.isFinite(v)?Math.clamp(v,min,max):fallback;}
    public static com.oculifelis.lararium.geometry.HollowShape.Criteria criteria(){return new com.oculifelis.lararium.geometry.HollowShape.Criteria(rule("min_cells",4,1,64),rule("spire_height",5,3,20),decimal("spire_ratio",1.4,.5,4),rule("vault_width",5,3,20),rule("vault_depth",3,2,20),rule("vault_height",3,2,10),rule("vault_cells",45,12,2048));}
    public static Optional<Weave> recipe(ItemStack s) { return WEAVES.entries().values().stream().filter(r->r.matches(s)).findFirst(); }
    public static void init() { RULES.id();WEAVES.id(); }
    private LarariumData() {}
}
