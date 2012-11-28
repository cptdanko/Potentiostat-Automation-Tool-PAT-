DATA Capture Automation Tool

This is a desktop based REST client for the ACData service used by School of Chemistry at UNSW for uploading the data generated from their experiments. The initial objective was to only have it work with the data generated from the PotentioStat, however the functionality was later expanded to include all possible instruments. 

Overview

The user/researcher first starts an experiment using the instrument, here the physical instrument is linked to a software, which produces the result (a bunch of files) to a specified directory. The user starts this program once the experiment is started and points it to the directory where the instrument will dump the results of the experiment. The program watches over this directory, until it receives a signal (a termination file) to stop the program and upload the files to the server.

Requirements

1) Java 6
2) Apache HTTP Client: Used to communicate with the web service
3) Apache Commons IO: This library was used to monitor the directory for incoming files
4) Netbeans 7.0 IDE: This IDE was used to reduce the development time by using the drag and drop GUI.
5) Jersey: This library was initially used to communicate with the REST client, but was later changed as it wasnt compatible with the spec of the web service.
6) Threading knowldege: Given that this is a GUI based desktop application, threading is used extensively for GUI updates as well as other functional aspects of the program.

Design overview

This program is composed of a set of classes each of which identify a certain functionality of the program. 

1) ACDataClient: This is the core interface that establishes the functionality that a typical ACData client provides. PATClient is an implmentation of this interface.

2) PatComponent: Almost every class inherits from this, as it gives them access to the system wide logger, the GUI frame and the properties resources bundle. In hindsight the MVC pattern should have been better used for this project.

3) FileWatcher: Provides the functionality for watching over a directory, PATFileWatcher is the class that provides this functionlity.

4) PATView: This is the Netbeans GUI form, which contains all the GUI components and hooks to access other functionality of the application from this.

5) Data classes: e.g. Sample, UserWork, Instrument and ExpDataUpload, these classes store the data that is retrieved from the ACData server.

6) Notifications: The Notify class contains a bunch of static methods which displays messages to the user given them feedback or action responses.
