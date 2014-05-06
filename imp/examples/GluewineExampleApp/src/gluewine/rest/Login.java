package gluewine.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.JOptionPane; 

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import gluewine.entities.User;

import org.gluewine.core.Glue;
import org.gluewine.persistence_jpa.Filter;
import org.gluewine.persistence_jpa.FilterLine;
import org.gluewine.persistence_jpa.FilterOperator;
import org.gluewine.persistence_jpa_hibernate.HibernateSessionProvider;
import org.gluewine.persistence.Transactional;
import org.gluewine.jetty.GluewineServlet;

public class Login extends GluewineServlet {
	
	@Override
 	public String getContextPath() {
 		return "login";
 	}
 	
 	@Glue
    private HibernateSessionProvider provider;

 	
 	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
 	{
 		resp.setContentType("text/html");

 		StringBuilder b = new StringBuilder(""
 				+ "<html>"
 				+ "	<head><title>Login</title>"
 				+ "	</head>"
 				+ "	<body>");
  		b.append("		<h1>Login</h1>");
  		b.append("			<form action='login' method='POST'>");
 		b.append("				<label for='username'>Username:</label>");
 		b.append("				<input type='text' name='username'/>");
 		b.append("				</br>");
 		b.append("				<label for='password'>Password:</label>");
 		b.append("				<input type='password' name='password'/>");
 		b.append("				</br>");
 		b.append("				<input type='submit' value='submit' name='submit'/>");
 		b.append("			</form>");
 		b.append("			<a href='http://localhost:8000/contacts/'>");
 		b.append("				<input type='button' value='Skip login'/>");
 		b.append("			</a>");
 		b.append("	</body>");
 		b.append("</html>");

 		resp.setContentLength(b.length());
 		try
 		{
 			resp.getWriter().println(b.toString());
 		}
 		catch (IOException e)
 		{
 			e.printStackTrace();
 			try
 			{
 				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server Error");
 		    }
 		    catch (IOException e1)
 		    {
 		    	e1.printStackTrace();
 		    }
 		}
	}
 	
 	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        
        resp.setContentType("text/html");
        
        List<gluewine.entities.User> users = provider.getSession().getAll(gluewine.entities.User.class);

        
        if (users.isEmpty()) {
        	resp.getWriter().write(""
        			+ "<html>"
        			+ "		<head>"
        			+ "			<title>Login</title>"
        			+ "		</head>"
        			+ "		<body>"
        			+ "			<h1>Testing</h1>"
        			+ "				<p> I'm terribly sorry, but no users were found.</p>"
        			+ "		</body>"
        			+ "</html>");
        }
        else {
        	for (gluewine.entities.User user : users)
            {
            	if (user.getUsername().equals(username) && user.getPassword().equals(password)) 
            	{
            		StringBuilder b = new StringBuilder(""
            				+ "<html>");
            		b.append("		<head>");
            		b.append("			<title> Gluewine framework </title>");
            		b.append("		</head>");
            		b.append("		<body>");
            		b.append("			<p>Welcome, " + username + " </p>");
            		b.append("		</body>");
            		b.append("</html>");
            		resp.setContentLength(b.length());
            	
	            	try {
	            		resp.getWriter().println(b.toString());
	            	}
	            	catch(IOException e)
	            	{
	            		e.printStackTrace();
	            		
	            		try {
	            			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server Error");
	            		}
	            		catch (IOException e1)
	            		{
	            			e1.printStackTrace();
	            		}
	            	}
            	}
            	else {
            		JOptionPane.showMessageDialog(null, "wrong username or password", "error", JOptionPane.ERROR_MESSAGE);
            		resp.sendRedirect("http://localhost:8000/login/");
            	}
            }//end for
        }// end else users.isEmpty()   
    }//end doPost
}
