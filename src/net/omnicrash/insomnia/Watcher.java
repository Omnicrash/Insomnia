package net.omnicrash.insomnia;

import org.bukkit.entity.Player;

import java.util.*;

class Watcher
  implements Runnable
{
    private Plugin plugin;

	private HashMap<Player, PlayerSleepState> _sleepingPlayers = new HashMap<Player, PlayerSleepState>();

    Watcher(Plugin plugin)
    {
    	this.plugin = plugin;

    }

	public void addSleepingPlayer(Player player, boolean daySleeping)
	{
		long currentTime = player.getWorld().getFullTime();

		PlayerSleepState state = new PlayerSleepState();
		state.player = player;
		state.bedTime = currentTime;
		state.healthLevel = player.getHealth();
		state.foodLevel = player.getFoodLevel();
		state.wellRested = false;
		state.daySleeping = daySleeping;

		_sleepingPlayers.put(player, state);

	}

	public PlayerSleepState getPlayerSleepState(Player player)
	{

		return _sleepingPlayers.get(player);
	}

	public void removeSleepingPlayer(Player player)
	{
		_sleepingPlayers.remove(player);

	}

	public void healAllSleepingPlayers()
	{
		final Iterator<Map.Entry<Player, PlayerSleepState>> playerIter = _sleepingPlayers.entrySet().iterator();
		while (playerIter.hasNext()) {
			PlayerSleepState state = playerIter.next().getValue();
			if (state.player != null && state.player.isOnline()) {
				state.player.setHealth(Util.getMaxHealth(state.player));
				Util.updatePlayerFoodLevel(state, plugin.foodCostPercentage);
				setWellRested(state);

			}

			playerIter.remove();
		}

	}

	public void cleanUp()
	{
		_sleepingPlayers.clear();

	}

    public void run()
    {
		final Iterator<Map.Entry<Player, PlayerSleepState>> playerIter = _sleepingPlayers.entrySet().iterator();
        while (playerIter.hasNext())
        {
        	// Remove the player if they are no longer present
			PlayerSleepState state = playerIter.next().getValue();
			if (state.player == null || !state.player.isOnline()) {
				playerIter.remove();
				continue;
			}

			if (state.wellRested) continue;

			// Apply healing until fully healed
        	heal(state);
        	
        }
        
    }
    
    private void heal(PlayerSleepState state)
    {
		double maxHealth = Util.getMaxHealth(state.player);
		double targetHealth = state.player.getHealth() + 1;
		if (targetHealth >= maxHealth) {
			targetHealth = maxHealth;
			setWellRested(state);
		}

        state.player.setHealth(targetHealth);
		Util.updatePlayerFoodLevel(state, plugin.foodCostPercentage);

    }

	private void setWellRested(PlayerSleepState state)
	{
		if (state.wellRested) return;

		if (!state.daySleeping) {
			// Do not set well rested before the minimum resting time
			long currentTime = state.player.getWorld().getFullTime();
			if (currentTime < (state.bedTime + Plugin.MIN_SLEEP_TICKS)) return;
		}

		state.player.sendMessage("You feel well rested.");
		state.wellRested = true;

	}
    public void setWellRested(Player player)
	{
		PlayerSleepState state = getPlayerSleepState(player);

		setWellRested(state);

	}

}