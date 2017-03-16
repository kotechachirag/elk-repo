package com.example;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController
public class Greeter {

    String greeting = "Test";

    int port = 8000;

    String projectName = "Elk projects";
    
    private final Logger log = LoggerFactory.getLogger(Greeter.class);

    @RequestMapping(value = "/", produces = "application/json")
    public List<String> index(){
        List<String> env = Arrays.asList(
                "message.greeting is: " + greeting,
                "server.port is: " + port,
                "configuration.projectName is: " + projectName
        );
        return env;
    }
    
    @RequestMapping(value = "/test", produces = "application/json")
    public List<String> indexTest() throws Exception{
    	log.info("Exception thrown from indexTest method");
        throw new Exception("Test Exception");
    }
}
