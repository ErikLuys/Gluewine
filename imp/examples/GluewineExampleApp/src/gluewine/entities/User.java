package gluewine.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "User")
public class User {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id = 0;
	
	@Column(name = "username")
	private String username;
	
	@Column(name = "password")
	private String password;
	 
	@Column(name = "isAdmin")
	private Boolean isAdmin;
	
	public User() {	}

	public long getId() 
	{
		return id;
	}

	public void setId(long id) 
	{
		this.id = id;
	}

	public String getUsername() 
	{
		return username;
	}

	public void setUsername(String username) 
	{
		this.username = username;
	}

	public String getPassword() 
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public Boolean getRole() 
	{
		return isAdmin;
	}

	public void setRole(Boolean role) 
	{
		this.isAdmin = isAdmin;
	}
}