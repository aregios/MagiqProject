<?php
    require_once("globals.php");
    
    function getInsertStatement($db) {
        $query = sprintf("insert into %s ( %s,  %s, %s,  %s,   %s) 
                                  values ('%s', %f, %f, '%s', '%s');", 
                mysqli_real_escape_string($db, TABLE_IMAGE_DATA),
                mysqli_real_escape_string($db, FLD_IMAGE_FILE),
                mysqli_real_escape_string($db, FLD_LATITUDE),
                mysqli_real_escape_string($db, FLD_LONGITUDE),
                mysqli_real_escape_string($db, FLD_DESCRIPTION),
                mysqli_real_escape_string($db, FLD_CLIENT_ID),
                mysqli_real_escape_string($db, $_POST[PKEY_IMAGE_FILE]),
                mysqli_real_escape_string($db, $_POST[PKEY_LATITUDE]),
                mysqli_real_escape_string($db, $_POST[PKEY_LONGITUDE]),
                mysqli_real_escape_string($db, $_POST[PKEY_DESCRIPTION]),
                mysqli_real_escape_string($db, $_POST[PKEY_CLIENT_ID]));
        
        return $query;
    }
    
    
    try {
        ini_set("display_errors", 0);                                                           //Suppress error reporting on web page 
        ini_set("log_errors", 1);                                                               //Enable error logging to file
        ini_set("error_log", ERROR_LOG);                                                        //Specify error log file
        
        if (!extension_loaded("exif")) {
            throw new Exception("Module not loaded");
        }
        checkUploadedFile(KEY_IMAGE_DATA);                                                      //Perform basic checks on the uploaded file
        
        $db = getDatabaseConnection();                                                          //Connect to the database    
        if (mysqli_connect_errno($db) != 0) {
            throw new Exception(mysqli_connect_error($db));
        }
        
        $insert = getInsertStatement($db);                                                      //Build insert SQL statement 
        mysqli_query($db, $insert);                                                             //Execute insert statement
        if (mysqli_errno($db) != 0) {
            throw new Exception(mysqli_error($db));
        }
            
        $id = (int)mysqli_insert_id($db);                                                       //Save returned autonumber id
        if ($id == 0) {
            throw new Exception("Invalid id (" . $id . ")");            
        }
        
        mysqli_close($db);                                                                      //Close database connection
        handleUploadedFile(DIR_IMAGE_DATA, KEY_IMAGE_DATA, $id);                                //Copy tmp file into ImageData directory

        echo "#ID" . $id . "#ID";                                                               //Return record id

    } catch (Exception $ex) {
        echo "#ERROR" . $ex->getMessage(). "#ERROR";                                            //Return Error message
    }
?>
