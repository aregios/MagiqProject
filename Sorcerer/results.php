<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" type="text/css" href="sorcerer.css" />
        <title></title>           
    </head>
    <body class="darkness textprimary">
        
    <?php
        require_once("globals.php");
        
        function getMasterQueryStatement($db, $m_id, $c_id) {
            $query = sprintf("select %s, %s, %s, %s, %s, %s, %s, %s, %s from %s 
                              where %s = %d and %s = '%s' limit 0, 1;",
                    mysqli_real_escape_string($db, FLD_ID),
                    mysqli_real_escape_string($db, FLD_IMAGE_FILE),
                    mysqli_real_escape_string($db, FLD_LATITUDE),
                    mysqli_real_escape_string($db, FLD_LONGITUDE),
                    mysqli_real_escape_string($db, FLD_DISTANCE),
                    mysqli_real_escape_string($db, FLD_MAX_TESTS),
                    mysqli_real_escape_string($db, FLD_COMPARE_METHOD),
                    mysqli_real_escape_string($db, FLD_REDUCE_FACTOR),
                    mysqli_real_escape_string($db, FLD_THRESHOLD),
                    mysqli_real_escape_string($db, TABLE_IMAGE_QUERIES),
                    mysqli_real_escape_string($db, FLD_ID),
                    mysqli_real_escape_string($db, $m_id),
                    mysqli_real_escape_string($db, FLD_CLIENT_ID),
                    mysqli_real_escape_string($db, $c_id));
            
            return $query;
        }
        
        
        function getDataQueryStatement($db, $m_latitude, $m_longitude, $m_distance, $m_maxtests) {
            $query = sprintf("select %s, %s, %s, %s, %s, geoDistance(%.6f, %.6f, %s, %s, %d) as %s from %s 
                              where geoDistance(%.6f, %.6f, %s, %s, %d) <= %d order by %s asc limit 0, %d;",
                    mysqli_real_escape_string($db, FLD_ID),
                    mysqli_real_escape_string($db, FLD_IMAGE_FILE),
                    mysqli_real_escape_string($db, FLD_DESCRIPTION),
                    mysqli_real_escape_string($db, FLD_LATITUDE),
                    mysqli_real_escape_string($db, FLD_LONGITUDE),
                    mysqli_real_escape_string($db, $m_latitude),
                    mysqli_real_escape_string($db, $m_longitude),
                    mysqli_real_escape_string($db, FLD_LATITUDE),
                    mysqli_real_escape_string($db, FLD_LONGITUDE),
                    mysqli_real_escape_string($db, EARTH_RADIUS_KM),                    
                    mysqli_real_escape_string($db, FLD_DISTANCE),
                    mysqli_real_escape_string($db, TABLE_IMAGE_DATA),
                    mysqli_real_escape_string($db, $m_latitude),
                    mysqli_real_escape_string($db, $m_longitude),
                    mysqli_real_escape_string($db, FLD_LATITUDE),
                    mysqli_real_escape_string($db, FLD_LONGITUDE),
                    mysqli_real_escape_string($db, EARTH_RADIUS_KM),                
                    mysqli_real_escape_string($db, $m_distance),
                    mysqli_real_escape_string($db, FLD_DISTANCE),
                    mysqli_real_escape_string($db, $m_maxtests));
            
            return $query; 
        }
        
        
        function getInsertResultsStatement($db, $m_id, $d_id, $result, $result_type) {
            $query = sprintf("insert into %s (%s, %s,   %s, %s) 
                                      values (%d, %d, %.6f, %d) on duplicate key 
                                      update %s = %.6f, %s = %d;",
                    mysqli_real_escape_string($db, TABLE_IMAGE_RESULTS),
                    mysqli_real_escape_string($db, FLD_MID),
                    mysqli_real_escape_string($db, FLD_DID),
                    mysqli_real_escape_string($db, FLD_RESULT),
                    mysqli_real_escape_string($db, FLD_RESULT_TYPE),
                    mysqli_real_escape_string($db, $m_id),
                    mysqli_real_escape_string($db, $d_id),
                    mysqli_real_escape_string($db, $result),
                    mysqli_real_escape_string($db, $result_type),
                    mysqli_real_escape_string($db, FLD_RESULT),
                    mysqli_real_escape_string($db, $result),
                    mysqli_real_escape_string($db, FLD_RESULT_TYPE),
                    mysqli_real_escape_string($db, $result_type));
            
            return $query;
        }
        
        
        try {        
            ini_set("display_errors", 0);                                                                       //Suppress error reporting on web page 
            ini_set("log_errors", 1);                                                                           //Enable error logging to file
            ini_set("error_log", ERROR_LOG);                                                                    //Specify error log file
            
            if (!extension_loaded("imcs2")) {
                throw new Exception("Module was not loaded");
            }

            if (!array_key_exists(GKEY_CID, $_GET)) {
                throw new Exception("Missing or invalid parameter value");
            }
            
            $c_id = $_GET[GKEY_CID];                                                                            //Get parameter (client id)
                        
            if (!array_key_exists(GKEY_ID, $_GET) || !is_numeric($_GET[GKEY_ID])) {
                throw new Exception("Missing or invalid parameter value");
            }
            
            $m_id = $_GET[GKEY_ID];                                                                             //Get parameter (master query selector)
                        
            if (!array_key_exists(GKEY_SW, $_GET) || !is_numeric($_GET[GKEY_SW])) {
                throw new Exception("Missing or invalid parameter value");
            }
            
            $sw = $_GET[GKEY_SW];                                                                               //Get parameter (device screen width => results table width)
            $tbw = round($sw * (96/100));                                                                       //Table width = device screen width minus a small margin
            $icw = round($tbw * (40/100));                                                                      //Images column = 40% of the table width
            
            $db = getDatabaseConnection();                                                                      //Connect to the database    
            if (mysqli_connect_errno($db) != 0) {
                throw new Exception(mysqli_connect_error($db));
            }
            
            $m_query = getMasterQueryStatement($db, $m_id, $c_id);                                              //Build master query
            $m_result = mysqli_query($db, $m_query);                                                            //Execute select statement
            if (mysqli_errno($db) != 0) {
                throw new Exception(mysqli_error($db));
            }
            
            if (mysqli_num_rows($m_result) == 0) {                                                              //No master record - something wrong happened
                throw new Exception("Invalid parameter value");            
            }
            
            $m_row = mysqli_fetch_array($m_result, MYSQLI_ASSOC);                                               //Read master query data (master row)
            $m_image_file = getAbsoluteFileName(DIR_IMAGE_QUERIES, $m_row[FLD_IMAGE_FILE], $m_row[FLD_ID]);     //Compose master image absolute name (file system)
            $m_latitude   = $m_row[FLD_LATITUDE];
            $m_longitude  = $m_row[FLD_LONGITUDE];
            $m_distance   = $m_row[FLD_DISTANCE] ;                                                              //Distance criterion 
            $m_maxtests   = $m_row[FLD_MAX_TESTS];
            $m_compare_method   = $m_row[FLD_COMPARE_METHOD];                                                   //Comparison rule
            $m_reduce_factor    = $m_row[FLD_REDUCE_FACTOR];                                                    //Comparison rule
            $m_threshold        = $m_row[FLD_THRESHOLD];                                                        //Image criterion
            mysqli_free_result($m_result);                                                                      //Release result set resources

            $imcs = new IMCSEngine();                                                                           //Create IMCS engine instance
            $m_imcs = $imcs->prepare($m_image_file, $m_compare_method, $m_reduce_factor);			//Prepare for comparison
            $m_imcs_status  = $m_imcs[IMCS_FLD_STATUS];     
            $m_imcs_message = $m_imcs[IMCS_FLD_MESSAGE];

            if ($m_imcs_status != 0) {
                throw new Exception($m_imcs_message); 
            }
            
            $d_query = getDataQueryStatement($db, $m_latitude, $m_longitude, $m_distance, $m_maxtests);         //Build geographical data query
            $d_result = mysqli_query($db, $d_query);                                                            //Execute select statement
            if (mysqli_errno($db) != 0) {
                throw new Exception(mysqli_error($db));
            }
            
            echo "<table style=" . quoteValue("width:" . $tbw) . ">";                                           //Table width
            $found = 0;                                                                                         //Reset counter
            while ($d_row = mysqli_fetch_array($d_result, MYSQLI_ASSOC)) {                                      //Read data row
                $d_image_file  = getAbsoluteFileName(DIR_IMAGE_DATA, $d_row[FLD_IMAGE_FILE], $d_row[FLD_ID]);   //Compose image absolute name (file system)
                $d_id          = $d_row[FLD_ID];                
                $d_latitude    = $d_row[FLD_LATITUDE];
                $d_longitude   = $d_row[FLD_LONGITUDE];
                $d_distance    = $d_row[FLD_DISTANCE];                                                          //Calculated distance
                $d_description = ifEmptyString($d_row[FLD_DESCRIPTION], DEFAULT_DESCRIPTION);                     
                
                $d_imcs = $imcs->compare($d_image_file);                                                        //Compare image
                $d_imcs_status      = $d_imcs[IMCS_FLD_STATUS];
                $d_imcs_message     = $d_imcs[IMCS_FLD_MESSAGE];
                $d_imcs_result      = $d_imcs[IMCS_FLD_RESULT];
                $d_imcs_result_type = $d_imcs[IMCS_FLD_RESULT_TYPE];
                
                if ($d_imcs_status != 0) {
                    if ($d_imcs_status == IMCS_ST_INVALID_IMAGE) {                                      
                        continue;                                                                               //Continue to next image
                    } else {
                        throw new Exception($d_imcs_message); 
                    }
                }
                
                if (imcs_filter_result($d_imcs_result, $m_threshold, $m_compare_method) == true) {              //Check comparison result against threshold - accept image as match
                    $found++;                                                                                   //Increment counter                    
                    $d_image_url = getAbsoluteFileURL(URL_IMAGE_DATA, $d_row[FLD_IMAGE_FILE], $d_row[FLD_ID]);  //Compose image file url
                    $d_details_url = "details.php?" . GKEY_MID . "=" . $m_id . "&"                              //Compose details page url
                                                    . GKEY_DID . "=" . $d_id . "&" 
                                                    . GKEY_SW  . "=" . $sw . "&"
                                                    . GKEY_CID . "=" . $c_id;
                    echo "<tr>";
                        echo "<td style=" . quoteValue("width:40%") . ">"; 
                            echo "<a href=" . quoteValue($d_details_url) . " target=" . quoteValue("_blank") . ">";
                            echo "<img class=" . quoteValue("cellimage") . " src=" . quoteValue($d_image_url) . " alt=" . quoteValue(DEFAULT_NOIMAGE) . getImageDimensions($d_image_file, $icw) . ">";
                            echo "</a>";
                        echo "</td>";
                        echo "<td>";
                            echo "<div>" . $d_description . "</div><br>";
                            echo "<div class=" . quoteValue("textsecondary") . ">lat: "  . sprintf("%.6f", $d_latitude)  . "</div>";
                            echo "<div class=" . quoteValue("textsecondary") . ">lon: "  . sprintf("%.6f", $d_longitude) . "</div>";
                            echo "<div class=" . quoteValue("textsecondary") . ">dist: " . sprintf("%dm", $d_distance)   . "</div>";
                            echo "<div class=" . quoteValue("textsecondary") . ">res: "  . imcs_describe_result($d_imcs_result, $d_imcs_result_type) . "</div>";
                        echo "</td>";
                    echo "</tr>";
                    
                    $insert = getInsertResultsStatement($db, $m_id, $d_id, $d_imcs_result, $d_imcs_result_type);    //Build insert SQL statement
                    mysqli_query($db, $insert);                                                                     //Execute insert statement
                    if (mysqli_errno($db) != 0) {
                        throw new Exception(mysqli_error($db));
                    }
                }
            }
            
            if ($found == 0) {
                echo "<tr>";
                    echo "<td class=" . quoteValue("cellmatches") . " colspan=" . quoteValue("2") . ">" ;
                        echo "<div>Sorry, no matches were found with given criteria.</div>";           
                    echo "</td>";
                echo "</tr>";                
            } else {
                $d_map_url = "map.php?" . GKEY_MID . "=" . $m_id . "&"
                                        . GKEY_CID . "=" . $c_id;  
                echo "<tr>";
                    echo "<td class=" . quoteValue("cellmatches") . " colspan=" . quoteValue("2") . ">" ;    
                        echo "<a href=" . quoteValue($d_map_url) . " target=" . quoteValue("_blank") . ">";
                        echo "Found " . $found . " matches";
                        echo "<img class = " . quoteValue("cellimageright") . " src=" . quoteValue("gmaps.jpg") . " width=25 height=25>";
                        echo "</a>";
                    echo "</td>";
                echo "</tr>";
                
                echo "<tr>";
                    echo "<td colspan=" . quoteValue("2") . ">" ;
                        echo "<br>";
                        echo "<div class=" . quoteValue("textsecondary") . ">Distance within: " . sprintf("%dm", $m_distance)                     . "</div>";
                        echo "<div class=" . quoteValue("textsecondary") . ">Compare method: "  . imcs_describe_compare_method($m_compare_method) . "</div>";
                        echo "<div class=" . quoteValue("textsecondary") . ">Reduce factor: "   . imcs_describe_reduce_factor($m_reduce_factor)   . "</div>";
                        echo "<div class=" . quoteValue("textsecondary") . ">Accept values: "   . imcs_describe_threshold($m_threshold, $m_compare_method) . "</div>";
                    echo "</td>";
                echo "</tr>";
            }
            echo "</table>";
            
            mysqli_free_result($d_result);                                                                      //Release result set resources                        
            mysqli_close($db);                                                                                  //Close database connection

        } catch (Exception $ex) {
            echo "Error: " . $ex->getMessage();
        }
        
    ?>
    </body>
</html>
