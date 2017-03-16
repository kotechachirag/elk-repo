# ELK Stack
  ELK stands for 
  * ElasticSearch
  * Logstash
  * Kibana

We need to install above three applications


###  Install Elasticsearch
*	Download elasticsearch zip file from https://www.elastic.co/downloads/elasticsearch
*	Extract it to a directory (unzip it)
*	Run it (bin/elasticsearch or bin/elasticsearch.bat on Windows)
*	Check that it runs using http://localhost:9200

If all is well, you should get the following result:
```
{
  "status" : 200,
  "name" : "Tartarus",
  "cluster_name" : "elasticsearch",
  "version" : {
    "number" : "1.7.1",
    "build_hash" : "b88f43fc40b0bcd7f173a1f9ee2e97816de80b19",
    "build_timestamp" : "2015-07-29T09:54:16Z",
    "build_snapshot" : false,
    "lucene_version" : "4.10.4"
  },
  "tagline" : "You Know, for Search"
}
```

### Install Kibana 4
*	Download Kibana archive from https://www.elastic.co/downloads/kibana 
o	Please note that you need to download appropriate distribution for your OS, URL given in examples below is for OS X
*	Extract the archive
*	Run it (bin/kibana)
*	Check that it runs by pointing the browser to the Kibana's WebUI
*	Point your browser to http://localhost:5601 (if Kibana page shows up, we're good - we'll configure it later)

### Install Logstash
*	Download Logstash zip from https://www.elastic.co/downloads/logstash
*	Extract it (unzip it)

### Configure Spring Boot's Log File
Anyhow, let's configure Spring Boot's log file. The simplest way to do this is to configure log file name in application.properties. It's enough to add the following line:
* logging.file=application.log
Spring Boot will now log ERROR, WARN and INFO level messages in the application.log log file and will also rotate it as it reaches 10 Mb.

### Configure Logstash to Understand Spring Boot's Log File Format
Now comes the tricky part. We need to create Logstash config file. Typical Logstash config file consists of three main sections: input, filter and output. Each section contains plugins that do relevant part of the processing (such as file input plugin that reads log events from a file or elasticsearch output plugin which sends log events to Elasticsearch).

* Input section
Input section defines from where Logstash will read input data - in our case it will be a file hence we will use a file plugin with multiline codec, which basically means that our input file may have multiple lines per log entry.

```
input {
  file {
    type => "java"
    path => "/path/to/application.log"
    codec => multiline {
      pattern => "^%{YEAR}-%{MONTHNUM}-%{MONTHDAY} %{TIME}.*"
      negate => "true"
      what => "previous"
    }
  }
}
```
* Filter Section
Filter section contains plugins that perform intermediary processing on an a log event. In our case, event will either be a single log line or multiline log event grouped according to the rules described above. In the filter section we will do several things:
	Tag a log event if it contains a stacktrace. This will be useful when searching for exceptions later on.
	Parse out (or grok, in logstash terminology) timestamp, log level, pid, thread, class name (logger actually) and log message.
	Specified timestamp field and format - Kibana will use that later for time based searches.

Filter section for Spring Boot's log format that aforementioned things looks like this:
```
filter {
  #If log line contains tab character followed by 'at' then we will tag that              entry as stacktrace
  if [message] =~ "\tat" {
    grok {
      match => ["message", "^(\tat)"]
      add_tag => ["stacktrace"]
    }
  }

  #Grokking Spring Boot's default log format
  grok {
    match => [ "message", 
               "(?<timestamp>%{YEAR}-%{MONTHNUM}-%{MONTHDAY} %{TIME})  %{LOGLEVEL:level} %{NUMBER:pid} --- \[(?<thread>[A-Za-z0-9-]+)\] [A-Za-z0-9.]*\.(?<class>[A-Za-z0-9#_]+)\s*:\s+(?<logmessage>.*)",
               "message",
               "(?<timestamp>%{YEAR}-%{MONTHNUM}-%{MONTHDAY} %{TIME})  %{LOGLEVEL:level} %{NUMBER:pid} --- .+? :\s+(?<logmessage>.*)"
             ]
  }

  #Parsing out timestamps which are in timestamp field thanks to previous grok section
  date {
    match => [ "timestamp" , "yyyy-MM-dd HH:mm:ss.SSS" ]
  }
}
```
* Ouptput Section
Output section contains output plugins that send event data to a particular destination. Outputs are the final stage in the event pipeline. We will be sending our log events to stdout (console output, for debugging) and to Elasticsearch.
Compared to filter section, output section is rather straightforward:
```
output {
  # Print each event to stdout, useful for debugging. Should be commented out in production.
  # Enabling 'rubydebug' codec on the stdout output will make logstash
  # pretty-print the entire event as something similar to a JSON representation.
  stdout {
    codec => rubydebug
  }

  # Sending properly parsed log events to elasticsearch
  elasticsearch {
  # elasticsearch is running in host
    hosts => "127.0.0.1"
  }
}
```
Sample File
Finally, the three parts - input, filter and output - need to be copy pasted together and saved into logstash.conf config file. Once the config file is in place and Elasticsearch is running, we can run Logstash:
/path/to/logstash/bin/logstash -f logstash.conf
If everything went well, Logstash is now shipping log events to Elasticsearch.

### Configure Kibana
Ok, now it's time to visit the Kibana web UI again. We have started it in step 2 and it should be running at http://localhost:5601. 
First, you need to point Kibana to Elasticsearch index(s) of your choice. Logstash creates indices with the name pattern of logstash-YYYY.MM.DD. In Kibana Settings → Indices configure the indices:
*	Index contains time-based events (select this option)
*	Use event times to create index names (select this option)
*	Index pattern interval: Daily
*	Index name or pattern: [logstash-]YYYY.MM.DD
*	Click on "Create Index"
Now click on "Discover" tab. In my opinion, "Discover" tab is really named incorrectly in Kibana - 
it should be labeled as "Search" instead of "Discover" because it allows you to perform new searches 
and also to save/manage them.
Log events should be showing up now in the main window. If they're not, then double check the time period filter 
in to right corner of the screen. 
Default table will have 2 columns by default: Time and _source. 
In order to make the listing more useful, we can configure the displayed columns.
From the menu on the left select level, class and logmessage.


