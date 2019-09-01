package net.omnicrash.insomnia;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

public class Util
{
	public static final int HOURS_TO_TICKS = 1000;
	public static final int SECONDS_TO_TICKS = 20;

	protected static Random _rnd = new Random();

	public static double getMaxHealth(Player player)
	{
		return player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
	}

	/**
	 * Checks if everyone in the world of the specified player is sleeping, with the exception of ignored players
	 * @param player
	 * @return
	 */
	public static boolean isWorldSleeping(Player player)
	{
		List<Player> worldPlayers = player.getWorld().getPlayers();

		int awakePlayers = worldPlayers.size();
		for (Player p : worldPlayers) {
			if (p == player || p.isSleeping() || p.isSleepingIgnored()) {
				awakePlayers--;
			}
		}

		return (awakePlayers == 0);
	}

	public static int getHoursRelative(long time)
	{
		return (int)(Math.floor(time / (double)HOURS_TO_TICKS)) % 24;
	}
	public static int getHoursAbsolute(long time)
	{
		return ((int)(Math.floor(time / (double)HOURS_TO_TICKS)) + 6) % 24;
	}
	public static int getMinutes(long time)
	{
		return (int)Math.ceil((time % HOURS_TO_TICKS) / ((double)HOURS_TO_TICKS) * 60);
	}

	public static String timeToStringRelative(long time)
	{
		int hours = getHoursRelative(time);
		int minutes = getMinutes(time);

		if (minutes == 0) {
			return String.format("%2dh", hours);
		}
		if (hours > 0) {
			return String.format("%2dh %2dm", hours, minutes);
		}
		return String.format("%2dm", minutes);
	}
	public static String timeToStringAbsolute(long time)
	{
		return String.format("%02d:%02d", getHoursAbsolute(time), getMinutes(time));
	}

	public static void updatePlayerFoodLevel(PlayerSleepState state, double foodCostPercentage)
	{
		if (foodCostPercentage <= 0) return;

		double healthHealed = Util.getMaxHealth(state.player) - state.healthLevel;
		int targetFoodLevel = (int)Math.ceil(state.foodLevel - (healthHealed * foodCostPercentage));
		targetFoodLevel = Math.max(targetFoodLevel, 0);
		state.player.setFoodLevel(targetFoodLevel);
		state.player.setSaturation(0.0f);
		state.player.setExhaustion(0.0f);

	}

	public static long getRandomTicks(double max)
	{
		return (long)(_rnd.nextDouble() * max);
	}

}
