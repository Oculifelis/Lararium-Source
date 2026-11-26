package com.oculifelis.lararium;

import com.fundamentum.moon.Moons;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

/** Two lit stitch lines leave the centre of the inhabitant visibly empty. */
public final class LarariumParticles {
    private static long budgetTick;
    private static int emitted;
    public static void tick(HollowHeartEntity h){
        Level l=h.getLevel();var p=Minecraft.getInstance().player;
        boolean greeting=h.form()==com.oculifelis.lararium.geometry.HollowShape.Form.NOOK&&h.getBlockState().getValue(HollowHeartBlock.PULSE);
        if(p==null||!h.getBlockState().getValue(HollowHeartBlock.AWAKE)||(h.breaths()<=0&&!greeting)||!h.getBlockPos().closerToCenterThan(p.position(),24))return;
        long now=l.getGameTime();if(now!=budgetTick){budgetTick=now;emitted=0;}if(emitted+6>64)return;
        if(h.form()==com.oculifelis.lararium.geometry.HollowShape.Form.LOOP&&!h.recording()&&!h.getBlockState().getValue(HollowHeartBlock.PULSE)&&now%3!=0)return;
        int[] colours={0xEFC58D,0x8EE0CF,0xBBA1EE,0xA7C8EE};int c=colours[h.form().ordinal()];
        var moon=Moons.current(l);
        if(moon.isPresent()&&moon.get().id().startsWith("ostentum:")){int accent=moon.get().id().contains("harvest")?0xF2B66E:moon.get().id().contains("glimmer")?0x85E0D7:0xD7A5EE;c=now%80<40?c:accent;}
        double phase=now*.065+Math.floorMod(h.getBlockPos().asLong(),83);
        for(int side=0;side<2;side++)for(int stitch=0;stitch<3;stitch++){
            double a=phase+side*Math.PI-stitch*.12,x,z,y;
            switch(h.form()){
                case NOOK->{double opening=greeting?.08:0;x=Math.cos(a)*(.28+opening);z=Math.sin(a)*(.24+opening);y=1.02+Math.sin(a*2)*.13+opening;}
                case SPIRE->{x=Math.cos(a)*.24;z=Math.sin(a)*.24;y=.8+(a/(Math.PI*2)-Math.floor(a/(Math.PI*2)))*1.05;}
                case LOOP->{double radius=h.getBlockState().getValue(HollowHeartBlock.PULSE)?.34:.25;x=Math.cos(a)*radius;z=Math.sin(a)*radius;y=1.12+Math.sin(a*4)*.035;}
                case VAULT->{x=Math.cos(a)*.43;z=Math.sin(a)*.16;y=.95+Math.sin(a)*.37;}
                default->throw new IllegalStateException("Unknown hollow form");
            }
            l.addParticle(new DustParticleOptions(c,.65F),h.getBlockPos().getX()+.5+x,h.getBlockPos().getY()+y,h.getBlockPos().getZ()+.5+z,0,.002,0);emitted++;
        }
    }
    private LarariumParticles(){}
}
