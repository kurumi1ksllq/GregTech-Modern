package com.gregtechceu.gtceu.common.item.debug;

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlockPatternPrinter {
    public RelativeDirection[] structureDir;
    public String[][] pattern;
    public int[][] aisleRepetitions;
    public Map<Character, Set<String>> symbolMap;

    public static final String CHARACTER_MAP = "abcedfghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public BlockPatternPrinter(Level level, BlockPos min, BlockPos max) {
        symbolMap = new HashMap<>();
        structureDir = new RelativeDirection[] { RelativeDirection.LEFT, RelativeDirection.UP, RelativeDirection.FRONT };
        pattern = new String[max.getX() - min.getX() + 1][max.getY() - min.getY() + 1];

        Map<BlockState, Character> map = new HashMap<>();
        map.put(Blocks.AIR.defaultBlockState(), ' ');

        int index = 0;

        for(int i = min.getX(); i <= max.getX(); i++) {
            for(int j = min.getY(); j <= max.getY(); j++) {
                StringBuilder builder = new StringBuilder();
                for(int k = min.getZ(); k <= max.getZ(); k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    BlockState state = level.getBlockState(pos);
                    if(!map.containsKey(state)) {
                        Character c = CHARACTER_MAP.charAt(index);
                        map.put(state, c);
                        symbolMap.putIfAbsent(c, new HashSet<>());
                        symbolMap.get(c).add((state.getBlock().toString()));
                        index++;
                    }
                    builder.append(map.get(state));
                }
                pattern[i - min.getX()][j - min.getY()] = builder.toString();
            }
        }
        var dirs = getDirection(Direction.NORTH);
        changeDir(dirs[0], dirs[1], dirs[2]);
    }

    public void changeDir(RelativeDirection charDir, RelativeDirection stringDir, RelativeDirection aisleDir) {
        if (charDir.isSameAxis(stringDir) || stringDir.isSameAxis(aisleDir) || aisleDir.isSameAxis(charDir)) return;

        int xi = structureDir[0].isSameAxis(aisleDir) ? pattern[0][0].length() :
                structureDir[1].isSameAxis(aisleDir) ? pattern[0].length : pattern.length;
        int yi = structureDir[0].isSameAxis(stringDir) ? pattern[0][0].length() :
                structureDir[1].isSameAxis(stringDir) ? pattern[0].length : pattern.length;
        int zi = structureDir[0].isSameAxis(charDir) ? pattern[0][0].length() :
                structureDir[1].isSameAxis(charDir) ? pattern[0].length : pattern.length;
        char[][][] newPattern = new char[xi][yi][zi];

        for(int i = 0; i < pattern.length; i++) {
            for(int j = 0; j < pattern[0].length; j++) {
                for(int k = 0; k < pattern[0][0].length(); k++) {
                    char c = pattern[i][j].charAt(k);
                    int x = 0, y = 0, z = 0;
                    if(structureDir[2].isSameAxis(aisleDir)) {
                        if(structureDir[2] == aisleDir) x = i;
                        else x = (pattern.length - 1) - i;
                    } else if(structureDir[2].isSameAxis(stringDir)) {
                        if(structureDir[2] == stringDir) y = i;
                        else y = (pattern.length - 1) - i;
                    } else if(structureDir[2].isSameAxis(charDir)) {
                        if(structureDir[2] == charDir) z = i;
                        else z = (pattern.length - 1) - i;
                    }

                    if(structureDir[1].isSameAxis(aisleDir)) {
                        if(structureDir[1] == aisleDir) x = j;
                        else x = (pattern[0].length - 1) - j;
                    } else if(structureDir[1].isSameAxis(stringDir)) {
                        if(structureDir[1] == stringDir) y = j;
                        else y = (pattern[0].length - 1) - j;
                    } else if(structureDir[1].isSameAxis(charDir)) {
                        if(structureDir[1] == charDir) z = j;
                        else z = (pattern[0].length - 1) - j;
                    }

                    if(structureDir[0].isSameAxis(aisleDir)) {
                        if(structureDir[0] == aisleDir) x = k;
                        else x = (pattern[0][0].length() - 1) - k;
                    } else if(structureDir[0].isSameAxis(stringDir)) {
                        if(structureDir[0] == stringDir) y = k;
                        else y = (pattern[0][0].length() - 1) - k;
                    } else if(structureDir[0].isSameAxis(charDir)) {
                        if(structureDir[0] == charDir) z = k;
                        else z = (pattern[0][0].length() - 1) - k;
                    }
                    newPattern[x][y][z] = c;
                }
            }
        }

        pattern = new String[newPattern.length][newPattern[0].length];
        for(int i = 0; i < pattern.length; i++) {
            for(int j = 0; j < pattern[0].length; j++) {
                StringBuilder builder = new StringBuilder();
                for(char c : newPattern[i][j]) builder.append(c);
                pattern[i][j] = builder.toString();
            }
        }

        structureDir = new RelativeDirection[] {charDir, stringDir, aisleDir};
    }

    public static RelativeDirection[] getDirection(Direction facing) {
        switch(facing) {
            case WEST -> {
                return new RelativeDirection[] {RelativeDirection.LEFT, RelativeDirection.UP, RelativeDirection.BACK};
            }
            case EAST -> {
                return new RelativeDirection[] {RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.FRONT};
            }
            case NORTH -> {
                return new RelativeDirection[] {RelativeDirection.BACK, RelativeDirection.UP, RelativeDirection.RIGHT};
            }
            case SOUTH -> {
                return new RelativeDirection[] {RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.LEFT};
            }
            case DOWN -> {
                return new RelativeDirection[] {RelativeDirection.RIGHT, RelativeDirection.BACK, RelativeDirection.UP};
            }
            default -> {
                return new RelativeDirection[] {RelativeDirection.LEFT, RelativeDirection.FRONT, RelativeDirection.UP};
            }
        }
    }
}
