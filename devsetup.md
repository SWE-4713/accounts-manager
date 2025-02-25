# Set up guide 
To set up your dev environment, do the following:

## 1. Initialize Docker Database  
Run `docker-compose up -d`  

## 2. Connect to database  
To see if the database is there, run `mysql -h 127.0.0.1 -P 3310 -u root -p`  
The default password is `devPassword` - rename it in the .env file  
Once connected to the container, type `show databases;` to see if any databases exit  
