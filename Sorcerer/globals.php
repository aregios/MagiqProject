<?php

    //Platform switch
    define("REMOTE", true); //true: run on remote server false: run on local host 

    
    //Absolute paths
    define("ERROR_LOG", dirname(__FILE__) . DIRECTORY_SEPARATOR . "error.log");                 //Error log file
    define("DIR_IMAGE_DATA", dirname(__FILE__) . DIRECTORY_SEPARATOR . "imagedata");            //Image data directory (server side usage)
    define("DIR_IMAGE_QUERIES", dirname(__FILE__) . DIRECTORY_SEPARATOR . "imagequeries");      //Image queries directory (server side usage)
 
    
    //Hyperlinks - Absolute paths
    define("URL_IMAGE_DATA", getBaseUrl() . "/imagedata");                                      //Image data directory URL (client side usage)
    define("URL_IMAGE_QUERIES", getBaseUrl() . "/imagequeries");                                //Image queries directory URL (client side usage)
    
    
    //$_FILES key
    define("KEY_IMAGE_DATA", "imagedata");                                                         
    
    
    //$_POST keys
    define("PKEY_IMAGE_FILE",     "imagefile");
    define("PKEY_LATITUDE",       "latitude");
    define("PKEY_LONGITUDE",      "longitude");
    define("PKEY_DESCRIPTION",    "description");
    define("PKEY_DISTANCE",       "distance");
    define("PKEY_MAX_TESTS",      "maxtests");
    define("PKEY_COMPARE_METHOD", "comparemethod");
    define("PKEY_REDUCE_FACTOR",  "reducefactor");
    define("PKEY_THRESHOLD",      "threshold");
    define("PKEY_CLIENT_ID",      "clientid");
    
    
    //$_GET keys
    define("GKEY_ID",  "id");                                                                   //Master query id
    define("GKEY_SW",  "sw");                                                                   //Device screen width in pixels
    define("GKEY_MID", "mid");                                                                  //Master query id (in master-details context)
    define("GKEY_DID", "did");                                                                  //Details id (in master-details context)  
    define("GKEY_CID", "cid");                                                                  //Client id
    
    
    //Database fields' names
    define("FLD_ID", "id");
    define("FLD_IMAGE_FILE",     "imagefile");
    define("FLD_LATITUDE",       "latitude");
    define("FLD_LONGITUDE",      "longitude");
    define("FLD_DESCRIPTION",    "description");
    define("FLD_DISTANCE",       "distance");
    define("FLD_MAX_TESTS",      "maxtests");
    define("FLD_COMPARE_METHOD", "comparemethod");
    define("FLD_REDUCE_FACTOR",  "reducefactor");
    define("FLD_THRESHOLD",      "threshold");
    define("FLD_CLIENT_ID",      "clientid");
    define("FLD_MID",            "mid");
    define("FLD_DID",            "did");
    define("FLD_RESULT",         "result");
    define("FLD_RESULT_TYPE",    "resulttype");
    
    
    //Database tables
    define("TABLE_IMAGE_DATA",    "imagedata");                                                 //MySQL data table
    define("TABLE_IMAGE_QUERIES", "imagequeries");                                              //MySQL queries table
    define("TABLE_IMAGE_RESULTS", "imageresults");                                              //MySQL results table

    
    //Miscellaneous values
    define("EARTH_RADIUS_KM", 6371);                                                            //Earth radius in KM approximation for geoDistance() calculations 
    define("DEFAULT_DESCRIPTION", htmlspecialchars("<No available description>"));              //Default no description message
    define("DEFAULT_NOIMAGE", htmlspecialchars("<No available image>"));                        //Default no image message
    
    
    //IMCS constants
    define("IMCS_FLD_STATUS",      "status");
    define("IMCS_FLD_MESSAGE",     "message");
    define("IMCS_FLD_RESULT",      "result");
    define("IMCS_FLD_RESULT_TYPE", "result_type");
    
    define("IMCS_ST_OK", 0);
    define("IMCS_ST_INVALID_IMAGE", 1);
    define("IMCS_ST_INVALID_PARAMETERS", 2);
    
    define("IMCS_RES_TYPE_VALUE", 1);
    define("IMCS_RES_TYPE_PERCENT", 100);
    
    define("IMCS_COMP_CORREL", 0);		
    define("IMCS_COMP_CHISQR", 1);		
    define("IMCS_COMP_INTERSECT", 2);		
    define("IMCS_COMP_BHATTACHARYYA", 3); 
    define("IMCS_COMP_EMD_L1", 4);		
    define("IMCS_COMP_EMD_L2", 5);		

    
    function getDatabaseConnection() {
        if (REMOTE == true)
            return mysqli_connect("83.212.111.60", "magiquser", "Pc3Wv5dmxCvCndSy", "magiq");   //Get a connection to the magic database
        else
            return mysqli_connect("127.0.0.1", "magiquser", "Pc3Wv5dmxCvCndSy", "magiq");       //Get a connection to the magic database
    }
    
    
    function getUploadErrorDescription($code) {
        switch ($code) {
            case UPLOAD_ERR_INI_SIZE:
            case UPLOAD_ERR_FORM_SIZE:
                $msg = "File is too large";
                break;

            case UPLOAD_ERR_PARTIAL:
                $msg = "File is incomplete";
                break;

            case UPLOAD_ERR_NO_FILE:
                $msg = "No file was uploaded";
                break;

            case UPLOAD_ERR_NO_TMP_DIR:
                $msg = "Upload directory not found";
                break;

            case UPLOAD_ERR_CANT_WRITE:
                $msg = "Cannot write file";
                break;

            case UPLOAD_ERR_EXTENSION:
                $msg = "Upload failed";
                break;

            default:
                $msg = "Unknown error";
        }

        return $msg;    
    }


    function checkUploadedFile($key) {
        if (!array_key_exists($key, $_FILES)) {
            throw new Exception("File not found in uploaded data");
        }

        $errCode = $_FILES[$key]["error"];
        if ($errCode != UPLOAD_ERR_OK) {
            throw new Exception(getUploadErrorDescription($errCode));
        }

        $tmpFile = $_FILES[$key]["tmp_name"];
        if (!is_uploaded_file($tmpFile)) {
            throw new Exception("File is not an uploaded file");
        } 

        if (!exif_imagetype($tmpFile)) {
            throw new Exception("File is not an image file");        
        }
    }


    function getUploadedFileInfoString($key) {
        $info =  "File Name: "  . $_FILES[$key]["name"]    . "<br>" .                           //Uploaded file name (without path)
                 "Type: "       . $_FILES[$key]["type"]    . "<br>" .                           //Uploaded file MIME type
                 "Size: "       . $_FILES[$key]["size"]    . "<br>" .                           //Uploaded file size
                 "Tmp File: "   . $_FILES[$key]["tmp_name"]. "<br>" .                           //Uploaded file temporary name (without path)
                 "Error Code: " . $_FILES[$key]["error"]   . "<br>";                            //Uploaded file status code
        
        return $info;
    }

    
    function getAbsoluteFileName($imageDir, $imageFile, $id) {
        return $imageDir . DIRECTORY_SEPARATOR . sprintf("%010d_", $id) . $imageFile;           //Create an absolute file name with NNNNNNNNNN_filename format
    }
    

    function handleUploadedFile($imageDir, $key, $id) {
        $sourceFile = $_FILES[$key]["tmp_name"];
        
        if (!file_exists($imageDir)) {
            if (is_writable(dirname($imageDir))) {
                mkdir($imageDir);
            } else {
                throw new Exception("Cannot create directory " . $imageDir);  
            }
        }
        
        $targetFile = getAbsoluteFileName($imageDir, $_FILES[$key]["name"], $id);
        if (!move_uploaded_file($sourceFile, $targetFile)) {
            throw new Exception("Cannot move file " . $sourceFile . " to " . $targetFile);
        }
        
        return $targetFile;
    }
    
    
    function getBaseUrl() {        
        $protocolPrefix = strtolower(substr($_SERVER["SERVER_PROTOCOL"], 0, 5));                //Get protocol prefix
        $protocol = ($protocolPrefix == "https") ? "https://" : "http://";                      //Get appropriate protocol prefix
        $hostName = $_SERVER["HTTP_HOST"];                                                      //Get host name
        $pathInfo = pathinfo($_SERVER["PHP_SELF"]);                                             //Get script file directory
        return $protocol . $hostName . $pathInfo["dirname"];                                    //Compose Url
    }

    
    function getAbsoluteFileURL($imageDirUrl, $imageFile, $id) {
        return $imageDirUrl . "/" . sprintf("%010d_", $id) . $imageFile;                        //Create an absolute URL with NNNNNNNNNN_filename format
    }

    
    function quoteValue($value) {
        return "\"" . $value . "\"";
    }
    
    
    function getImageDimensions($imageFile, $fitWidth) {
        $size = getimagesize($imageFile);
        if ($size) {
            $iw = $size[0];                                                                                     //Image width
            $ih = $size[1];                                                                                     //Image height
            if ($iw <= $fitWidth) {                                                                             //Image fits in space
                return " width=" . quoteValue($iw) . " height=" . quoteValue($ih);                              //Return attributes
            } else {
                return " width=" . quoteValue($fitWidth) . " height=" . quoteValue(($fitWidth * ($ih / $iw)));  //Adjust dimensions and return attributes
            }
        } else {
            return " width=" . quoteValue($fitWidth);
        }
    }
    
    
    function ifEmptyString($value, $default) {
        if (trim($value) == "")
            return $default;
        else
            return trim($value);
    }
    
    
    function imcs_filter_result($result, $threshold, $compare_method) {
        
        //No threshold
        if ($threshold == 0) {
            return true;
        }
        
        //Threshold is a minimum
        if ($compare_method == IMCS_COMP_CORREL || $compare_method == IMCS_COMP_BHATTACHARYYA || $compare_method == IMCS_COMP_INTERSECT) {
            if ($threshold > 0 && $result >= $threshold) {
                return true;
            }
        }
        
        //Threshold is a maximum
        if ($compare_method == IMCS_COMP_CHISQR || $compare_method == IMCS_COMP_EMD_L1 || $compare_method == IMCS_COMP_EMD_L2) {
            if ($threshold > 0 && $result <= $threshold) {
                return true;
            }
        }
        
        return false;
    }

    
    function imcs_describe_result($result, $result_type) {
        if ($result_type == IMCS_RES_TYPE_PERCENT)
            return sprintf("%.2f%%", ($result * 100));
        else
            return sprintf("%.3f", $result);
    }
    
    
    function imcs_describe_compare_method($compare_method) {
        $arr_compare_method = array("Correlation", 
                                    "Chi-square",
                                    "Intersection",
                                    "Bhattacharyya distance",
                                    "EMD Manhattan distance",
                                    "EMD Euclidean distance");
        
        return $arr_compare_method[$compare_method];    
    }
    
    
    function imcs_describe_reduce_factor($reduce_factor) {
        if ($reduce_factor > 0)
            return sprintf("%d per channel",  $reduce_factor);
        else          
            return "none";
    } 
    
    
    function imcs_describe_threshold($threshold, $compare_method) {
        
        //No threshold
        if ($threshold == 0) {
            return "all";
        }
        
        //Threshold is a minimum
        if ($compare_method == IMCS_COMP_CORREL || $compare_method == IMCS_COMP_BHATTACHARYYA || $compare_method == IMCS_COMP_INTERSECT) {
            return sprintf("over %.2f%%", ($threshold * 100));
        }
        
        //Threshold is a maximum
        if ($compare_method == IMCS_COMP_CHISQR || $compare_method == IMCS_COMP_EMD_L1 || $compare_method == IMCS_COMP_EMD_L2) {
            return sprintf("less than %.3f", $threshold);
        }
        
        return "";
    }

?>
