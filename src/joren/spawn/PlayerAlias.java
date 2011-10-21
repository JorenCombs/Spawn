package joren.spawn;

import org.bukkit.entity.Player;

public class PlayerAlias {
	private Player[] people;
	private String params;
	
	public PlayerAlias()
	{
		people = new Player[0];
		params = "";
	}
	
	public PlayerAlias(Player[] people, String params)
	{
		this.people=people;
		this.params=params;
	}
	
	public Player[] getPeople()
	{
		return people;
	}

	public void setPeople(Player[] people)
	{
		this.people=people;
	}

	public String getParams()
	{
		return params;
	}

	public void setParams(String params)
	{
		this.params=params;
	}
}