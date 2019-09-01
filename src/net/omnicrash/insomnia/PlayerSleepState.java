package net.omnicrash.insomnia;

import org.bukkit.entity.Player;

class PlayerSleepState
{
    Player player;
    long bedTime;
    double healthLevel;
    int foodLevel;
    boolean wellRested = false;
    boolean daySleeping = false;
}
