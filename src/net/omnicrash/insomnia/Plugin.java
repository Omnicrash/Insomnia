package net.omnicrash.insomnia;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Event.Result;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedEnterEvent.BedEnterResult;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.entity.Player;

public class Plugin extends JavaPlugin implements Listener
{
	public final Logger log = Logger.getLogger("Minecraft");

	public final Watcher watcher = new Watcher(this);

	public final static int MIN_SLEEP_TICKS = 100; // default sleep is 101 ticks

	// Config options
	public boolean defaultDaySkip;
	public double timeSkipHoursMin;
	public double timeSkipHoursRange;
	public boolean allowDaySleep;
	public double foodCostPercentage;
	public double sleepCooldownHours;
	public int napCooldownMinutes;

	private long _targetTime;
	private boolean _allSleeping = false;

	private HashMap<Player, Long> _cooldown = new HashMap<Player, Long>();

	@Override
	public void onEnable()
	{
		getServer().getPluginManager().registerEvents(this, this);

		// Create config if needed
		this.saveDefaultConfig();

		// Fetch config parameters
		defaultDaySkip = getConfig().getBoolean("general.default-day-skip");
		List<Double> timeSkipHours = getConfig().getDoubleList("general.time-skip-hours");
		if (timeSkipHours.size() == 0) {
			timeSkipHoursMin = 0;
			timeSkipHoursRange = 0;

		} else if (timeSkipHours.size() == 1) {
			timeSkipHoursMin = timeSkipHours.get(0);
			timeSkipHoursRange = 0;

		} else if (timeSkipHours.size() == 2) {
			timeSkipHoursMin = timeSkipHours.get(0);
			timeSkipHoursRange = timeSkipHours.get(1) - timeSkipHoursMin;
			if (timeSkipHoursRange < 0) {
				log.log(Level.WARNING, "Invalid time range! Check your time-skip-hours parameter");
			}

		} else {
			log.log(Level.WARNING, "Invalid amount of parameters in time range! Check your time-skip-hours parameter");

		}

		allowDaySleep = getConfig().getBoolean("general.allow-day-sleep");
		foodCostPercentage = getConfig().getDouble("general.food-cost-percentage");
		sleepCooldownHours = getConfig().getDouble("general.sleep-cooldown-hours");
		napCooldownMinutes = getConfig().getInt("general.nap-cooldown-minutes");

		// Start the update thread
		getServer().getScheduler().scheduleSyncRepeatingTask(this, watcher, Util.SECONDS_TO_TICKS, Util.SECONDS_TO_TICKS);

	}

	@Override
	public void onDisable()
	{
		watcher.cleanUp();

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerBedEnterEvent(PlayerBedEnterEvent e)
	{
		BedEnterResult result = e.getBedEnterResult();

		boolean sleeping = (result == BedEnterResult.OK);
		boolean daySleeping = false;

		// Allow sleeping during the day
		if (allowDaySleep) {
			if (result == BedEnterResult.NOT_POSSIBLE_NOW) {
				e.setUseBed(Result.ALLOW);
				sleeping = true;
				daySleeping = true;

			}

		}

		if (!sleeping) return;

		Player player = e.getPlayer();
		long currentTime = player.getWorld().getFullTime();

		// Check cooldown
		if (_cooldown.containsKey(player)) {
			long cooldownExpiration = _cooldown.get(player);
			if (currentTime < cooldownExpiration) {
				player.sendMessage("You aren't tired enough to sleep... Try again in " + Util.timeToStringRelative(cooldownExpiration - currentTime));
				e.setUseBed(Result.DENY);

				return;
			}

			_cooldown.remove(player);
		}

		// Start regeneration
		watcher.addSleepingPlayer(player, daySleeping);

		if (defaultDaySkip) return;

		if (Util.isWorldSleeping(player)) {
			_targetTime = currentTime + (int)(timeSkipHoursMin * Util.HOURS_TO_TICKS);
			_targetTime += Util.getRandomTicks(timeSkipHoursRange * Util.HOURS_TO_TICKS);
			_allSleeping = true;

		}

	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerBedLeaveEvent(PlayerBedLeaveEvent e)
	{
		Player player = e.getPlayer();
		PlayerSleepState state = watcher.getPlayerSleepState(player);
		World world = player.getWorld();
		long currentTime = world.getFullTime();

		if (!state.daySleeping && (currentTime < (state.bedTime + MIN_SLEEP_TICKS))) {
			// Sleep was interrupted
			watcher.removeSleepingPlayer(player);
			_allSleeping = false;

		} else if (defaultDaySkip) {
			// Make sure the player has full health whenever they exit the bed
			player.setHealth(Util.getMaxHealth(player));
			Util.updatePlayerFoodLevel(state, foodCostPercentage);
			watcher.removeSleepingPlayer(player);
			watcher.setWellRested(player);

		} else if (_allSleeping) {
			// Time-skip, give all players full health
			watcher.healAllSleepingPlayers();
			world.setFullTime(_targetTime);
			currentTime = _targetTime;
			_allSleeping = false;

		}

		long restingTime = currentTime - state.bedTime;

		// If the player has fully rested, start their cooldown
		if (state.wellRested) {
			if (sleepCooldownHours > 0) {
				_cooldown.put(player, currentTime + (long)(sleepCooldownHours * Util.HOURS_TO_TICKS));
			}

			player.sendMessage("You wake up at " + Util.timeToStringAbsolute(currentTime) + " after sleeping for " + Util.timeToStringRelative(restingTime));

		} else {
			if (napCooldownMinutes > 0) {
				_cooldown.put(player, currentTime + (long)(napCooldownMinutes * Util.HOURS_TO_TICKS / 60));
			}

			player.sendMessage("You get up at " + Util.timeToStringAbsolute(currentTime) + " after a brief " + Util.getMinutes(restingTime) + " minute nap");

		}

	}

}
