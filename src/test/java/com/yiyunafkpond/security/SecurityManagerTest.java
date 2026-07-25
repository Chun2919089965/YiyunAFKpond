package com.yiyunafkpond.security;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityManagerTest {

    @Test
    void ipLimitDoesNotCountThePlayerBeingChecked() {
        UUID currentPlayer = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();

        assertEquals(0, SecurityManager.countOtherPlayers(Set.of(currentPlayer), currentPlayer));
        assertEquals(1, SecurityManager.countOtherPlayers(
                Set.of(currentPlayer, otherPlayer), currentPlayer));
    }

    @Test
    void handlesMissingIpPoolIndex() {
        assertEquals(0, SecurityManager.countOtherPlayers(null, UUID.randomUUID()));
        assertEquals(0, SecurityManager.countOtherPlayers(Set.of(), UUID.randomUUID()));
    }
}
