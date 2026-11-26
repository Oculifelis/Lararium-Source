package com.oculifelis.lararium.geometry;

import java.util.*;

/** Bounded, incremental measurement. This class never loads a chunk or holds a world. */
public final class HollowShape {
    public record Cell(int x, int y, int z) {
        public Cell add(int dx, int dy, int dz) { return new Cell(x+dx,y+dy,z+dz); }
    }
    public enum Material { AIR, WALL, UNLOADED }
    public enum Form { NOOK, SPIRE, LOOP, VAULT }
    public record Criteria(int minimum,int spireHeight,double spireRatio,int vaultWidth,int vaultDepth,int vaultHeight,int vaultVolume){
        public static final Criteria DEFAULT=new Criteria(4,5,1.4,5,3,3,45);
    }
    public interface Reader { Material at(Cell cell); }
    public record Result(Set<Cell> cells, Form form, String problem, int width, int height, int depth) {
        public boolean valid() { return problem.isEmpty(); }
        public static Result failed(String why) { return new Result(Set.of(),Form.NOOK,why,0,0,0); }
    }
    public static final class Scan {
        private final Reader reader;
        private final int range, height, limit;
        private final Criteria criteria;
        private final ArrayDeque<Cell> todo=new ArrayDeque<>();
        private final Set<Cell> queued=new HashSet<>(), air=new HashSet<>();
        private final Map<Cell,Cell> parents=new HashMap<>();
        private Cell failureAt;
        private Result result;
        private int samples;
        public Scan(Reader reader,int range,int height,int limit) {
            this(reader,range,height,limit,Criteria.DEFAULT);
        }
        public Scan(Reader reader,int range,int height,int limit,Criteria criteria) {
            this.reader=reader; this.range=range; this.height=height; this.limit=limit;
            this.criteria=criteria;
            queue(new Cell(0,0,0));
        }
        private void queue(Cell p) { if(queued.add(p)) todo.add(p); }
        public int samples() { return samples; }
        public boolean done() { return result!=null; }
        public Result result() { return result; }
        public List<Cell> diagnosticPath() {
            if(failureAt==null)return List.of();
            List<Cell> path=new ArrayList<>();for(Cell p=failureAt;p!=null;p=parents.get(p))path.add(p);
            Collections.reverse(path);return List.copyOf(path);
        }
        private void queueFrom(Cell p,Cell parent){if(!queued.contains(p)){parents.put(p,parent);queue(p);}}
        public void step(int budget) {
            for(int i=0;i<budget && !done() && !todo.isEmpty();i++) {
                Cell p=todo.remove();
                // Inspect the boundary cell before declaring a leak: an outer wall closes the space.
                Material m=reader.at(p); samples++;
                if(m==Material.UNLOADED) { failureAt=p;result=Result.failed("unloaded"); break; }
                if(m==Material.WALL) continue;
                if(Math.abs(p.x)>range || Math.abs(p.z)>range || Math.abs(p.y)>=height) {
                    failureAt=p;result=Result.failed("range"); break;
                }
                air.add(p);
                if(air.size()>limit) { failureAt=p;result=Result.failed("large"); break; }
                queueFrom(p.add(1,0,0),p);queueFrom(p.add(-1,0,0),p);queueFrom(p.add(0,1,0),p);
                queueFrom(p.add(0,-1,0),p);queueFrom(p.add(0,0,1),p);queueFrom(p.add(0,0,-1),p);
            }
            if(!done() && todo.isEmpty()) result=classify(air,criteria);
        }
    }
    public static Result classify(Set<Cell> air) {
        return classify(air,Criteria.DEFAULT);
    }
    public static Result classify(Set<Cell> air,Criteria criteria) {
        if(air.size()<criteria.minimum) return Result.failed("small");
        int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,minY=minX,maxY=maxX,minZ=minX,maxZ=maxX;
        for(Cell p:air) { minX=Math.min(minX,p.x);maxX=Math.max(maxX,p.x);minY=Math.min(minY,p.y);maxY=Math.max(maxY,p.y);minZ=Math.min(minZ,p.z);maxZ=Math.max(maxZ,p.z); }
        int w=maxX-minX+1,h=maxY-minY+1,d=maxZ-minZ+1;
        Form form=Form.NOOK;
        if(hasWalkableLoop(air,minX,maxX,minY,maxY,minZ,maxZ)) form=Form.LOOP;
        else if(h>=criteria.spireHeight && h>=Math.max(w,d)*criteria.spireRatio) form=Form.SPIRE;
        else if(Math.max(w,d)>=criteria.vaultWidth && Math.min(w,d)>=criteria.vaultDepth && h<=criteria.vaultHeight && air.size()>=criteria.vaultVolume) form=Form.VAULT;
        return new Result(Set.copyOf(air),form,"",w,h,d);
    }
    private static boolean hasWalkableLoop(Set<Cell> air,int minX,int maxX,int minY,int maxY,int minZ,int maxZ) {
        if(maxX-minX<2 || maxZ-minZ<2 || maxY==minY) return false;
        for(int y=minY;y<maxY;y++) {
            Set<Cell> passage=new HashSet<>();
            for(Cell p:air) if(p.y==y && air.contains(p.add(0,1,0))) passage.add(new Cell(p.x,0,p.z));
            // Flood the complement from outside. A remaining island is enclosed by a two-high passage.
            Set<Cell> outside=new HashSet<>(); ArrayDeque<Cell> q=new ArrayDeque<>();
            Cell start=new Cell(minX-1,0,minZ-1);q.add(start);outside.add(start);
            while(!q.isEmpty()) {
                Cell p=q.remove();
                for(int[] v:DIR2) {
                    Cell n=p.add(v[0],0,v[1]);
                    if(n.x<minX-1 || n.x>maxX+1 || n.z<minZ-1 || n.z>maxZ+1 || passage.contains(n))continue;
                    if(outside.add(n))q.add(n);
                }
            }
            for(int x=minX+1;x<maxX;x++)for(int z=minZ+1;z<maxZ;z++) {
                Cell p=new Cell(x,0,z);
                // A one-block furnishing (including the heart itself) is not a pillar.
                if(!outside.contains(p) && !air.contains(new Cell(x,y,z)) && !air.contains(new Cell(x,y+1,z))) return true;
            }
        }
        return false;
    }
    private static final int[][] DIR2={{1,0},{-1,0},{0,1},{0,-1}};
    private HollowShape() {}
}
