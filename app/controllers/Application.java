package controllers;

import play.*;
import play.db.jpa.JPA;
import play.mvc.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import jobs.StudentLoader;
import models.*;

public class Application extends Controller {

	@Before
	public static void checkUser() {
		if (request.action.equals("Application.youAreNotWithUs")) {
			return;
		}
		if (request.cookies.get("JSESSIONID") == null) {
            session.clear();
    		redirect("http://" + request.host + "?app=unifaces");
		};
		String auth = checkAuth(request.host, request.cookies.get("JSESSIONID").value);
		if (auth.equals("anon")) {
			redirect("http://" + request.host + "?app=unifaces");
		}
    	Student student = Student.find("email = ?", auth).first();
    	if (student == null) {
    		youAreNotWithUs();
    	}
    	LocalUser localUser;
    	if (student.localUser == null) {
    		LocalUser newLocalUser = new LocalUser();
    		newLocalUser.points = 0;
    		newLocalUser.student = student;
    		newLocalUser.save();
    		localUser = newLocalUser;
    	} else {
    		localUser = student.localUser;
    	}
		session.put("userid", localUser.id);
	}
	
    public static void index() {
    	LocalUser localUser = LocalUser.findById(Integer.valueOf(session.get("userid")));
    	Question question = getQuestion(localUser);
    	render(question, localUser);
    }
    
    public static void top() {
    	List<LocalUser> localUsers = JPA.em().createQuery("Select u from LocalUser u order by points desc", LocalUser.class).setMaxResults(20).getResultList();
    	render(localUsers);
    }
    
    public static void getPhoto(int questionId) {
    	Question question = Question.findById(questionId);
    	if (question != null) {
	    	try {
	    		File file = new File(Play.applicationPath.getCanonicalPath().toString() + "/public/photos/" + question.rightAnswer + ".jpg");
				if (file.exists()) {
					renderBinary(file);
					return;
				}
	    	} catch (IOException e) {}
    	}
    	try {
			renderBinary(new File(Play.applicationPath.getCanonicalPath().toString() + "/public/photos/no.jpg"));
		} catch (IOException e) {}
    }
    
    public static void next() {
    	LocalUser localUser = LocalUser.findById(Integer.valueOf(session.get("userid")));
    	if (localUser == null) {
    		renderText("login");
    	}
    	Question question = getQuestion(localUser);
    	renderText(question.toJson());
    }
    
    public static void youAreNotWithUs() {
    	renderText("NU has deleted you from its databases or for some reason you are not in unifaces, sorry :(");
    }
    
    public static void answer(int questionId, int answer) {
    	LocalUser localUser = LocalUser.findById(Integer.valueOf(session.get("userid")));
    	if (localUser == null) {
    		renderText("login");
    	}
    	Question question = Question.findById(questionId);
    	if (question == null || question.answered || question.localUser.id != localUser.id) {
    		renderText("error");
    	}
    	if (question.createdDate != null && late(question.createdDate)) {
    		renderText("error");
    	}
    	question.answered = true;
    	question.save();
    	Student right = Student.findById(question.rightAnswer);
    	String info = right.name + " " + right.school + " " + right.nuid.substring(0, 4);
    	if (question.rightAnswer == answer) {
    		if (localUser.points < 9999999) {
    			localUser.points += 20;
    		}
    		localUser.lastQuestion = null;
    		localUser.save();
    		renderText("{\"questionId\": \"" + questionId + "\", \"correct\": \"true\", \"info\": \"" + info + "\"}");
    	} else {
    		if (localUser.points > -9999999) {
    			localUser.points -= 10;
    		}
    		localUser.lastQuestion = null;
    		localUser.save();
    		renderText("{\"questionId\": \"" + questionId + "\", \"correct\": \"false\", \"info\": \"" + info + "\"}");
    	}
    }
    
    private static Question getQuestion(LocalUser localUser) {
    	if (localUser.lastQuestion != null) {
    		localUser.points -=10;
    	}
    	List<Student> students = StudentLoader.students;
    	Random random = new Random();
    	Student chosen = students.get(random.nextInt(students.size()));
    	Student chosen2 = null;
    	do {
    		chosen2 = students.get(random.nextInt(students.size()));
    	} while (chosen2.id == chosen.id || chosen2.isMale != chosen.isMale);
    	Student chosen3 = null;
    	do {
    		chosen3 = students.get(random.nextInt(students.size()));
    	} while (chosen3.id == chosen2.id || chosen3.id == chosen.id || chosen3.isMale != chosen.isMale);
    	Student chosen4 = null;
    	do {
    		chosen4 = students.get(random.nextInt(students.size()));
    	} while (chosen4.id == chosen3.id || chosen4.id == chosen2.id || chosen4.id == chosen.id || chosen4.isMale != chosen.isMale);
    	Question question = new Question(chosen, chosen2, chosen3, chosen4, localUser);
    	question.save();
    	localUser.lastQuestion = question;
    	localUser.save();
    	return question;
    }
    
    private static String checkAuth(String host, String cookie) {
    	HttpURLConnection connection = null;
    	try {
			URL url = new URL("http://127.0.0.1:8080/loggeduser");
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Host", host);
			connection.setRequestProperty("Cookie", "JSESSIONID=" + cookie);
			connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			connection.setDoInput(true);
			InputStream is = connection.getInputStream();
		    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		    StringBuilder response = new StringBuilder();
		    String line;
		    while ((line = rd.readLine()) != null) {
		      response.append(line);
		    }
		    rd.close();
		    return response.toString();
		} catch (Exception e) {
			return ("anon");
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
    }
    
    private static boolean late(Date date) {
    	Date date2 = new Date();
    	long duration  = date2.getTime() - date.getTime();
    	long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(duration);
    	if (diffInSeconds > 10) {
    		return true;
    	} else {
    		return false;
    	}
    }
}