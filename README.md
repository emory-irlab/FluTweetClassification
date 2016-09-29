# Emory Twitter Life Event Detector

##Created by Alec Wolyniec, Asher Mouat, and Timothy Lee
##With invaluable help from Dr. Eugene Agichtein, Dr. Phillip Wolff, and Denis Savenkov

###Last updated: Sept. 29th, 2016

##Overview
The Emory Twitter Life Event Detector is a tool for detecting a small range of life events
(well-defined classes of events in a person's life, such as birth or marriage) in data from Twitter. It provides a 
set of pre-trained classifiers, a interface on which to run them and extract tweets containing life events, and an
interface that allows you to train and run your own classifiers, given your own training data and our model.

The current version, currently in the testing stage, aims to carry out the following functions:
    *   Classify a random tweet as being authored by a human being writing on their own behalf or a human/bot 
    writing for an organization
    *   Identify person-written tweets that contain life events
    *   Determine whether a life event in a tweet is being experienced by the tweet's author or by somebody
    else (other)
    
##Implementation
The following life events are supported:
    *   Major trip (vacation, business, religious, etc.)
    *   School graduation
    *   Job loss (whether a firing or a layoff)
    *   New job (a recent hire or the beginning of work)
    *   Major illness (such as a cold, the flu, and heart disease)
    *   Recovery from major illness
    
##How to Run

##Libraries

##Methodology and Performance

##Project Structure
