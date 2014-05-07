package gluewine.rest;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gluewine.core.Glue;
import org.gluewine.jetty.GluewineServlet;
import org.gluewine.persistence.Transactional;
import org.gluewine.persistence_jpa_hibernate.HibernateSessionProvider;

import gluewine.entities.Contact;

public class ModifyContact extends GluewineServlet {

	@Override
	public String getContextPath() {
		return "modifycontact";
	}
	
	@Glue
    private HibernateSessionProvider provider;
	
	@Transactional
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
        resp.setContentType("text/html");
        
        StringBuilder b = new StringBuilder(""
        		+"<html>");
        b.append(" 	<head> ");
        b.append("		<title> Adminpanel </title> ");
        b.append("		<link rel='stylesheet' type='text/css' href='style.css' />");
        b.append("		<style type='text/css'>"
        		+ "				a:link { color: #000000; text-decoration: none; }"
        		+ "				.btn { border-radius:6px; text-indent:-1.08px; border:1px solid #dcdcdc; display:inline-block; color:#777777; font-family:arial; font-size:15px; font-weight:bold; font-style:normal; height:50px; line-height:50px; width:200px; text-decoration:none; text-align:center;}"
        		+ "				.lbl { width:120px; display: block; float: left; font-family:arial; }"
        		+ "				.inpt { width:250px; font-family:arial; }"
        		+ "				.h1 { width:100%; background-color:#a80321; height:20%; color:#ffffff; text-align:center; font-family:arial; }"	
        		+ "		</style>");        		
        b.append("  </head>");
        b.append("	<body>");
        b.append("		<h1 class='h1'>Modify contact</h1>");
        b.append("			<form action='ModifyContact' method='POST'>");
        b.append("				<label for='firstname' class='lbl'>Firstname:</label>");
        b.append("				<input type='text' name='firstname' class='inpt'/>");
        b.append("				</br>");
        b.append("				<label for='lastname' class='lbl'>Lastname:</label>");
        b.append("				<input type='lastname' name='lastname' class='inpt'/>");
        b.append("				</br>");
        b.append("				<label for='email' class='lbl'>Email Adress:</label>");
        b.append("				<input type='text' name='email' class='inpt'/>");
        b.append("				</br>");
        b.append("				<label for= 'phone' class='lbl'>Phone:</label>");
        b.append("				<input type='text' name='phone' class='inpt'/>");
        b.append("				</br></br>");
        b.append("				<a href='http://localhost:8000/adminpanel/'>");
 		b.append("					<input type='button' value='<- Back' class='btn'/>");
 		b.append("				</a>");
        b.append("				<input type='submit' value='Modify contact' name='submit' class='btn'/>");
        b.append("			</form>");   
        
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
	
	@Transactional
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        
    }
}