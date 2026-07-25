package com.yiyunafkpond.pond;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PondTest {

    @Test
    void includesBothIntegerConfigurationEndpoints() {
        Pond pond = pond(new Location(null, 0, 60, 0), new Location(null, 10, 70, 10));

        assertEquals(0, pond.getMinX());
        assertEquals(10, pond.getMaxX());
        assertEquals(60, pond.getMinY());
        assertEquals(70, pond.getMaxY());
        assertEquals(1_331, pond.getSize());
    }

    @Test
    void sameHeightSelectionStillContainsOneBlockLayer() {
        World world = world("world");
        Pond pond = new Pond("test", "Test", world,
                new Location(world, 0.5, 64, 0.5),
                new Location(world, 10.5, 64, 10.5));

        assertEquals(64, pond.getMinY());
        assertEquals(64, pond.getMaxY());
        assertEquals(121, pond.getSize());
        assertTrue(pond.isInPond(new Location(world, 10.9, 64, 10.9)));
        assertFalse(pond.isInPond(new Location(world, 10.9, 65, 10.9)));
    }

    @Test
    void normalizesReversedEndpointsByBlockCoordinate() {
        Pond pond = pond(new Location(null, 8.5, 72, -2.5), new Location(null, 3.5, 68, -7.5));

        assertEquals(3, pond.getMinX());
        assertEquals(8, pond.getMaxX());
        assertEquals(68, pond.getMinY());
        assertEquals(72, pond.getMaxY());
        assertEquals(-8, pond.getMinZ());
        assertEquals(-3, pond.getMaxZ());
        assertEquals(180, pond.getSize());
    }

    private static Pond pond(Location first, Location second) {
        return new Pond("test", "Test", null, first, second);
    }

    private static World world(String name) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "World[" + name + "]";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
