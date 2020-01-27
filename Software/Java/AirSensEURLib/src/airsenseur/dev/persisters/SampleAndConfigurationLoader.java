/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package airsenseur.dev.persisters;

import airsenseur.dev.exceptions.PersisterException;
import airsenseur.dev.json.BoardInfo;
import airsenseur.dev.json.SensorConfig;
import java.util.List;

/**
 *
 * @author marco
 */
public interface SampleAndConfigurationLoader extends SampleLoader {
    
    public long getSensorInfoMinimumTimestamp() throws PersisterException;
    public long getSensorInfoMaximumTimestamp() throws PersisterException;
    public long getBoardInfoMinimumTimestamp() throws PersisterException;
    public long getBoardInfoMaximumTimestmp() throws PersisterException;
    
    public List<BoardInfo> loadBoardInfo(long firstTimeStamp, long lastTimeStamp) throws PersisterException;
    public List<SensorConfig> loadSensorInfo(long firstTimeStamp, long lastTimeStamp) throws PersisterException;
}
