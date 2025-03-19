package noppes.npcs.reflection;

import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.LogWriter;

import java.lang.reflect.Field;

public class PathReflection {

    private static Field points;

    private static Field openSet;

    private static Field closedSet;

    private static Field currentPathIndex;

    public static PathPoint[] getPoints(Path path) {
        if (path == null) { return new PathPoint[0]; }
        if (points == null) {
            try {
                try { points = Path.class.getDeclaredField("field_75884_a"); }
                catch (Exception e) { points = Path.class.getDeclaredField("points"); }
            } catch (Exception e) {
                LogWriter.error("Error found field \"points\"", e);
            }
        }
        try {
            Field field = getOpenSetField();
            field.setAccessible(true);
            return (PathPoint[]) field.get(path);
        } catch (Exception e) {
            LogWriter.error("Error get \"points\" in " + path, e);
        }
        return new PathPoint[0];
    }

    public static PathPoint[] getOpenSet(Path path) {
        if (path == null) { return new PathPoint[0]; }
        try {
            Field field = getOpenSetField();
            field.setAccessible(true);
            return (PathPoint[]) field.get(path);
        } catch (Exception e) {
            LogWriter.error("Error get \"openSet\" in " + path, e);
        }
        return new PathPoint[0];
    }

    public static PathPoint[] getClosedSet(Path path) {
        if (path == null) { return new PathPoint[0]; }
        try {
            Field field = getClosedSetField();
            field.setAccessible(true);
            return (PathPoint[]) field.get(path);
        } catch (Exception e) {
            LogWriter.error("Error get \"closedSet\" in " + path, e);
        }
        return new PathPoint[0];
    }

    public static int getCurrentPathIndex(Path path) {
        if (path == null) { return 0; }
        try {
            Field field = getCurrentPathIndexField();
            field.setAccessible(true);
            return (int) field.get(path);
        } catch (Exception e) {
            LogWriter.error("Error get \"currentPathIndex\" in " + path, e);
        }
        return 0;
    }

    public static void setOpenSet(Path path, PathPoint[] newOpenSet) {
        if (path == null || newOpenSet == null) { return; }
        try {
            Field field = getOpenSetField();
            field.setAccessible(true);
            field.set(path, newOpenSet);
        } catch (Exception e) {
            LogWriter.error("Error set \"openSet\" in " + path, e);
        }
    }

    public static void setClosedSet(Path path, PathPoint[] newClosedSet) {
        if (path == null || closedSet == null) { return; }
        try {
            Field field = getClosedSetField();
            field.setAccessible(true);
            field.set(path, newClosedSet);
        } catch (Exception e) {
            LogWriter.error("Error set \"closedSet\" in " + path, e);
        }
    }

    public static void setCurrentPathIndex(Path path, int newCurrentPathIndex) {
        if (path == null || newCurrentPathIndex < 0) { return; }
        try {
            Field field = getCurrentPathIndexField();
            field.setAccessible(true);
            field.set(path, newCurrentPathIndex);
        } catch (Exception e) {
            LogWriter.error("Error set \"currentPathIndex\" in " + path, e);
        }
    }

    private static Field getOpenSetField() {
        if (openSet == null) {
            try {
                try { openSet = Path.class.getDeclaredField("field_186312_b"); }
                catch (Exception e) { openSet = Path.class.getDeclaredField("openSet"); }
            } catch (Exception e) {
                LogWriter.error("Error found field \"openSet\"", e);
            }
        }
        return openSet;
    }

    private static Field getClosedSetField() {
        if (closedSet == null) {
            try {
                try { closedSet = Path.class.getDeclaredField("field_186313_c"); }
                catch (Exception e) { closedSet = Path.class.getDeclaredField("closedSet"); }
            } catch (Exception e) {
                LogWriter.error("Error found field \"closedSet\"", e);
            }
        }
        return closedSet;
    }

    private static Field getCurrentPathIndexField() {
        if (currentPathIndex == null) {
            try {
                try { currentPathIndex = Path.class.getDeclaredField("field_75882_b"); }
                catch (Exception e) { currentPathIndex = Path.class.getDeclaredField("currentPathIndex"); }
            } catch (Exception e) {
                LogWriter.error("Error found field \"currentPathIndex\"", e);
            }
        }
        return currentPathIndex;
    }

}
