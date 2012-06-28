package com.x.biz;
import org.springframework.context.support.ClassPathXmlApplicationContext;
public final class Launcher {

	    private Launcher() {
	        // hide utility class constructor
	    }

	    public static void main(final String[] args) {
	        new ClassPathXmlApplicationContext("jetty.xml");
	    }
}
