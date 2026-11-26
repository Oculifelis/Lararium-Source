package com.oculifelis.lararium.geometry;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.oculifelis.lararium.geometry.HollowShape.*;

class HollowShapeTest {
    private Set<Cell> box(int w,int h,int d) {
        Set<Cell>s=new HashSet<>();for(int x=0;x<w;x++)for(int y=0;y<h;y++)for(int z=0;z<d;z++)s.add(new Cell(x,y,z));return s;
    }
    private Result scan(Set<Cell> cells) {
        Scan s=new Scan(p->cells.contains(p)?Material.AIR:Material.WALL,8,12,512);
        while(!s.done())s.step(7); return s.result();
    }
    @Test void compactRoom() { var r=scan(box(3,3,3));assertTrue(r.valid());assertEquals(Form.NOOK,r.form());assertEquals(27,r.cells().size()); }
    @Test void tallShaft() { assertEquals(Form.SPIRE,scan(box(3,7,3)).form()); }
    @Test void lowVault() { assertEquals(Form.VAULT,scan(box(6,3,4)).form()); }
    @Test void rotatingAVaultKeepsItsForm() { assertEquals(Form.VAULT,scan(box(4,3,6)).form()); }
    @Test void lowFurnitureIsNotAPillar() { var s=box(3,3,3);s.remove(new Cell(1,0,1));assertEquals(Form.NOOK,scan(s).form()); }
    @Test void heartInTheMiddleOfAFloorIsNotAPillar() {
        Set<Cell> s=new HashSet<>();for(var p:box(3,3,3))s.add(p.add(-1,-1,-1));s.remove(new Cell(0,-1,0));
        assertEquals(Form.NOOK,scan(s).form());
    }
    @Test void ringNeedsHeadroom() {
        var s=box(5,3,5);s.removeIf(p->p.x()==2&&p.z()==2);assertEquals(Form.LOOP,scan(s).form());
        s=box(5,1,5);s.remove(new Cell(2,0,2));assertNotEquals(Form.LOOP,scan(s).form());
    }
    @Test void openingIsRecoverable() { Scan s=new Scan(p->Material.AIR,3,6,512);while(!s.done())s.step(4);assertEquals("range",s.result().problem());assertFalse(s.diagnosticPath().isEmpty()); }
    @Test void unboundedRoomStopsAtCellLimit() { Scan s=new Scan(p->Material.AIR,100,100,16);while(!s.done())s.step(2);assertFalse(s.result().valid());assertTrue(s.samples()<100); }
    @Test void unloadedBoundaryIsNotAir() { Scan s=new Scan(p->Material.UNLOADED,8,12,512);s.step(2);assertEquals("unloaded",s.result().problem());assertEquals(1,s.samples()); }
    @Test void budgetIsAHardLimit() { Scan s=new Scan(p->Material.AIR,8,12,512);s.step(5);assertEquals(5,s.samples()); }
    @Test void sealedEnvelopeAtRangeIsAccepted() { assertTrue(scan(box(8,3,3)).valid()); }
    @Test void resultCannotBeMutated() { var r=scan(box(2,2,2));assertThrows(UnsupportedOperationException.class,()->r.cells().clear()); }
    @Test void deadEndIsNotALoop() { var s=box(5,3,5);s.removeIf(p->p.x()==2&&p.z()>=2);assertNotEquals(Form.LOOP,scan(s).form()); }
    @Test void seedMustHaveUsableSpace() { assertEquals("small",scan(Set.of()).problem()); }
    @Test void heartCanSitOnFloorAboveTheLowestAirCells(){var s=new HashSet<Cell>();for(var p:box(3,3,3))s.add(p.add(0,-1,0));s.remove(new Cell(0,-1,0));var r=scan(s);assertTrue(r.valid());assertEquals(3,r.height());}
    @Test void escapingTrailIsAConnectedRoute(){Scan s=new Scan(p->Material.AIR,3,6,512);while(!s.done())s.step(3);var path=s.diagnosticPath();assertEquals(new Cell(0,0,0),path.getFirst());for(int i=1;i<path.size();i++){var a=path.get(i-1);var b=path.get(i);assertEquals(1,Math.abs(a.x()-b.x())+Math.abs(a.y()-b.y())+Math.abs(a.z()-b.z()));}}
}
