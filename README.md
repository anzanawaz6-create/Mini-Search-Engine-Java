# Mini Search Engine (Java)

## Overview

Mini Search Engine is a Java-based information retrieval system that indexes text documents using an Inverted Index data structure. The project supports efficient keyword searching, Boolean queries (AND/OR), word frequency tracking, and document retrieval.

## Features

* Document Parsing and Tokenization
* Inverted Index using HashMap
* Stop Word Removal
* Word Frequency Counting
* Single Keyword Search
* AND Query Support
* OR Query Support
* Multi-keyword AND Queries
* Hyphenated Word Handling
* Full Document Content Display
* Interactive Command Line Interface

## Technologies Used

* Java
* HashMap
* HashSet
* ArrayList
* File Handling
* Object-Oriented Programming (OOP)

## Project Structure

Mini_Search_Engine/
│
├── src/
│   └── Main.java
│
├── documents/
│   ├── Doc1.txt
│   ├── Doc2.txt
│   └── Doc50.txt
│
└── README.md

## How It Works

### Phase 1: Document Parsing

The system reads all documents and tokenizes the text into searchable words.

### Phase 2: Index Construction

An Inverted Index is created where:

Word → List of Documents Containing the Word

Example:

flow → Doc1(4), Doc7(2), Doc15(1)

### Phase 3: Query Processing

The system supports:

Single Word Search:
flow

AND Query:
flow AND stream

OR Query:
velocity OR speed

Multi-AND Query:
boundary AND layer AND flow

### Phase 4: Result Display

Matching documents are displayed along with their complete content.

## Running the Project

1. Clone the repository

git clone https://github.com/anzanawaz6-create/Mini-Search-Engine-Java.git

2. Open the project in IntelliJ IDEA

3. Run Main.java

4. Enter search queries in the console

## Example Queries

flow

flow AND stream

velocity OR speed

boundary AND layer

showindex

exit

## Learning Outcomes

This project demonstrates:

* Inverted Index Construction
* Information Retrieval Concepts
* HashMap-Based Search Optimization
* Boolean Query Processing
* Java Collections Framework
* File Processing and Parsing
  

Anza Nawaz

BS SoftWare Engineering
