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
        
        
        function getDataQueryStatement($db, $m_latitude, $m_longitude, $d_id) {
            $query = sprintf("select %s, %s, %s, %s, %s, geoDistance(%.6f, %.6f, %s, %s, %d) as %s from %s 
                              where %s = %d limit 0, 1;",
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
                    mysqli_real_escape_string($db, FLD_ID),
                    mysqli_real_escape_string($db, $d_id));
            
            return $query; 
        }
        
        
        function getResultsStatement($db, $m_id, $d_id) {
            $query = sprintf("select %s, %s from %s 
                              where %s = %d and %s = %d limit 0, 1",
                    mysqli_real_escape_string($db, FLD_RESULT),
                    mysqli_real_escape_string($db, FLD_RESULT_TYPE),                    
                    mysqli_real_escape_string($db, TABLE_IMAGE_RESULTS),
                    mysqli_real_escape_string($db, FLD_MID),
                    mysqli_real_escape_string($db, $m_id),
                    mysqli_real_escape_string($db, FLD_DID),
                    mysqli_real_escape_string($db, $d_id));
            
            return $query;
        }
        
        
        try {        
            ini_set("display_errors", 0);                                                                       //Suppress error reporting on web page 
            ini_set("log_errors", 1);                                                                           //Enable error logging to file
            ini_set("error_log", ERROR_LOG);                                                                    //Specify error log file
            
            if (!array_key_exists(GKEY_CID, $_GET)) {
                throw new Exception("Missing or invalid parameter value");
            }
            
            $c_id = $_GET[GKEY_CID];                                                                            //Get parameter (client id)
            
            if (!array_key_exists(GKEY_MID, $_GET) || !is_numeric($_GET[GKEY_MID])) {
                throw new Exception("Missing or invalid parameter value");
            }
            
            $m_id = $_GET[GKEY_MID];                                                                            //Get parameter (master query selector)
            
            if (!array_key_exists(GKEY_DID, $_GET) || !is_numeric($_GET[GKEY_DID])) {
                throw new Exception("Missing or invalid parameter value");
            }
            
            $d_id = $_GET[GKEY_DID];                                                                            //Get parameter (data record selector)
            
            if (!array_key_exists(GKEY_SW, $_GET) || !is_numeric($_GET[GKEY_SW])) {
                throw new Exception("Missing or invalid parameter value");
            }
            
            $sw = $_GET[GKEY_SW];                                                                               //Get parameter (device screen width => results table width)
            $tbw = round($sw * (96/100));                                                                       //Table width = device screen width minus a small margin
            
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
            $m_image_url  = getAbsoluteFileURL(URL_IMAGE_QUERIES, $m_row[FLD_IMAGE_FILE], $m_row[FLD_ID]);      //Compose image file url            
            $m_latitude   = $m_row[FLD_LATITUDE];
            $m_longitude  = $m_row[FLD_LONGITUDE];
            $m_distance   = $m_row[FLD_DISTANCE] ;                                                              //Distance criterion 
            $m_compare_method   = $m_row[FLD_COMPARE_METHOD];                                                   //Comparison rule
            $m_reduce_factor    = $m_row[FLD_REDUCE_FACTOR];                                                    //Comparison rule
            $m_threshold        = $m_row[FLD_THRESHOLD];                                                        //Image criterion
            mysqli_free_result($m_result);                                                                      //Release result set resources

            $d_query = getDataQueryStatement($db, $m_latitude, $m_longitude, $d_id);                            //Build details data query
            $d_result = mysqli_query($db, $d_query);                                                            //Execute select statement
            if (mysqli_errno($db) != 0) {
                throw new Exception(mysqli_error($db));
            }
            
            if (mysqli_num_rows($d_result) == 0) {                                                              //No details record - something wrong happened
                throw new Exception("Invalid parameter value");            
            }
            
            $d_row = mysqli_fetch_array($d_result, MYSQLI_ASSOC);                                               //Read data row
            $d_image_file  = getAbsoluteFileName(DIR_IMAGE_DATA, $d_row[FLD_IMAGE_FILE], $d_row[FLD_ID]);       //Compose image absolute name (file system)
            $d_image_url   = getAbsoluteFileURL(URL_IMAGE_DATA, $d_row[FLD_IMAGE_FILE], $d_row[FLD_ID]);        //Compose image file url            
            $d_latitude    = $d_row[FLD_LATITUDE];
            $d_longitude   = $d_row[FLD_LONGITUDE];
            $d_distance    = $d_row[FLD_DISTANCE];                                                              //Calculated distance
            $d_description = ifEmptyString($d_row[FLD_DESCRIPTION], DEFAULT_DESCRIPTION);
            mysqli_free_result($d_result);                                                                      //Release result set resources
            
            $r_query = getResultsStatement($db, $m_id, $d_id);                                                  //Build result value query
            $r_result = mysqli_query($db, $r_query);                                                            //Execute select statement
            if (mysqli_errno($db) != 0) {
                throw new Exception(mysqli_error($db));
            }

            if (mysqli_num_rows($r_result) == 0) {
                $d_imcs_result = 0;
                $d_imcs_result_type = 0;
            } else {
                $r_row = mysqli_fetch_array($r_result, MYSQLI_ASSOC);                                           //Read data row
                $d_imcs_result      = $r_row[FLD_RESULT];
                $d_imcs_result_type = $r_row[FLD_RESULT_TYPE];
            }
            
            mysqli_free_result($r_result);                                                                      //Release result set resources  
            mysqli_close($db);                                                                                  //Close database connection

            echo "<table style=" . quoteValue("width:" . $tbw) . ">";                                           //Table width
                echo "<tr>";
                    echo "<td>";
                        echo "<div>Matched Image</div>";
                    echo "</td>";
                echo "</tr>";
                
                echo "<tr>";
                    echo "<td>"; 
                        echo "<img class=" . quoteValue("cellimage") . " src=" . quoteValue($d_image_url) . " alt=" . quoteValue(DEFAULT_NOIMAGE) . getImageDimensions($d_image_file, $tbw) . ">";
                    echo "</td>";
                echo "</tr>";
                
                echo "<tr>";
                    echo "<td>";
                        echo "<div class=" . quoteValue("textsecondary") . ">"       . $d_description                . "</div>";
                        echo "<div class=" . quoteValue("textsecondary") . ">lat: "  . sprintf("%.6f", $d_latitude)  . "</div>";
                        echo "<div class=" . quoteValue("textsecondary") . ">lon: "  . sprintf("%.6f", $d_longitude) . "</div>";
                        echo "<div class=" . quoteValue("textsecondary") . ">dist: " . sprintf("%dm", $d_distance)   . "</div>";
                        echo "<div class=" . quoteValue("textsecondary") . ">res: "  . imcs_describe_result($d_imcs_result, $d_imcs_result_type) . "</div>";
                    echo "</td>";
                echo "</tr>";

                echo "<tr><td><br></td></tr>";

                echo "<tr>";
                    echo "<td>";
                        echo "<div>Master Image</div>";
                    echo "</td>";
                echo "</tr>";
                
                echo "<tr>";
                    echo "<td>"; 
                        echo "<img class=" . quoteValue("cellimage") . " src=" . quoteValue($m_image_url) . " alt=" . quoteValue(DEFAULT_NOIMAGE) . getImageDimensions($m_image_file, $tbw) . ">";
                    echo "</td>";
                echo "</tr>";
                
                echo "<tr>";
                    echo "<td>";
                        echo "<div class=" . quoteValue("textsecondary") . ">lat: " . sprintf("%.6f", $m_latitude)  . "</div>";
                        echo "<div class=" . quoteValue("textsecondary") . ">lon: " . sprintf("%.6f", $m_longitude) . "</div>";
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
            echo "</table>";
            
        } catch (Exception $ex) {
            echo "Error: " . $ex->getMessage();
        }
        
    ?>
    </body>
</html>
