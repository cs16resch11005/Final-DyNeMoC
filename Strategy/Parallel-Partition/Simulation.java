import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Simulation 
{
	/* Total number of processors or cores available to the JVM */
	int numCores = Runtime.getRuntime().availableProcessors();
	
	public static Vector<Agent> population = new Vector<Agent>();	
	public static Vector<City>  nodes      = new Vector<City>();
	public static Vector<HCU>  hcus        = new Vector<HCU>();
	public static Vector<Partition>  partitions   = new Vector<Partition>();

	public static int [][] countDiffAgents = new int[(ConfigParameters.num_Nodes)][3]; 
	public static int [] sampleInfect      = new int[ConfigParameters.num_Nodes];
	
	public static int numHCUs               = 0;
	public static int currentTime           = 0;
	
	public static int queryExpressions[][];
	public static int operandPositions[];
		
	public static String modQuery;	
	public static String output; 
	
	public  RecoverInfectedAgents ria; 
	public  ModelEpidemic me;      
	
	public boolean isQuerySatisfied;
	
	 
	Simulation(int _numHCUs)
	{
		
		numHCUs         = _numHCUs;		
		output          = ConfigParameters.path_output_folder + "Parallel-Partition.txt";		      		      
		currentTime     = 0;
		isQuerySatisfied        = false;		
		ria             = new RecoverInfectedAgents();
		me              = new ModelEpidemic();
		population.clear();
		nodes.clear();
		hcus.clear();   
		partitions.clear();	
	}
	
	public void preConfiguration() throws IOException 
	{
		//open file for writing simulation output
		File fe = new File(output);	    
	    
		if(fe.exists())
	      fe.delete();
		
		for(int i=0; i<ConfigParameters.num_People; i++)
		{
			Agent agent = new Agent(i);		
			
			agent.attributes = new HashMap<Integer, Integer>();	
			ModelEpidemic.initializeAgentAttributes(agent);
			
			agent.nbrList = new ArrayList<Integer>();			
			ModelEpidemic.initializeAgentNeighborList(agent, InitialSetup.hm.get(i));
			
			population.add(agent);
		}	
		
		for(int i=0; i<ConfigParameters.num_Nodes; i++)
		{
			City city = new City(i);
			city.people = new Vector<Integer>();
			nodes.add(city);
		}
		
		ModelEpidemic.initialCityPopulation(nodes, InitialSetup.hm);
		
		for(int i=0; i<numCores; i++)
		{
			Partition partition = new Partition(i);	
			partition.listOfAgents = new Vector<Integer>();
			partitions.add(partition);			
		}		
		//initialize people in each partition
		ModelEpidemic.initializePartitions(partitions);
		//numHCUs = 3;
		
		for(int i=0; i<numHCUs; i++)
		{
			HCU hcu = new HCU(i);
			hcus.add(hcu);
		}				
		
		ria.initialPositionsOfHCUs(hcus);	
		
		modQuery = readAndParseQuery();
		
		Run();		
	}/* End of preConfiguration() */	
	
		
	public void preRun() throws IOException
	{
		me.findInfectedAtEachCity(output, numHCUs, population, nodes, countDiffAgents, sampleInfect);
		
		recoverInfectedAgentsWithHCU();
		
		for(int i=0; i<population.size(); i++)
		{
			ModelEpidemic.countInfectedNeighborsOfAgent(population.get(i));
		}
		
	}/* End of preRun()*/
	
	public void nextPositionsOfHCUs() 
	{		
		ria.determinePositionsOfHCUBasedOnStrategy(hcus);
	}/* End of nextPositionsOfHCUs()*/

	public void recoverInfectedAgentsWithHCU() throws IOException 
	{
		for(int i=0; i<hcus.size(); i++)
		{
			int location = hcus.get(i).currLocation;			
		    ria.clearInfectedAgentsWithHCU(population, nodes.get(location).people, hcus.get(i), location);
		}
	}/* End of recoverInfectedAgentsWithHCU() */

	public void Run() throws IOException
	{
		//ConfigParameters.sim_Time
		for(int i=1; i<ConfigParameters.sim_Time; i++)
		{
			currentTime = i;
			preRun();
			ModelEpidemic.generateRandomNumbers();					
			nextStateOfAgents();
			nextPositionOfAgents();			
			postRun(i);
			
			if(isQuerySatisfied)
				break;
		}			
		
	}/* End of Run()*/

	public void postRun(int time) throws IOException
	{
				
		clearAllCityInformation();
		
		ModelEpidemic.updateCityPopulation(population, nodes);
		
		clearAllAgentNeighborInformation();		
		
		nextNeighborsListOfAgents();
		
		nextPositionsOfHCUs();	
		
		if(checkTerminationOfSimulation())
		{
			System.out.println("Simulation Has Completed !! ");			
			isQuerySatisfied = true;
		}
		
		/*if(checkQuerySatisfied())
		{
			System.out.println("Query Has Satisfied !! ");
			System.exit(1);
		}*/		
		
	}/* End of postRun()*/
	
	
	public boolean checkQuerySatisfied()
	{
		/*EvaluateExpression ee = new EvaluateExpression();		
		
		modQuery = ee.evaluateAtomicPropositions(operandPositions.length, modQuery, operandPositions, queryExpressions, population, nodes);	
				
		if(operandPositions.length == 1)
		{
			if(ee.evaluateSingleAtomicProposition(modQuery))
				return true;
			else
				return false;
		}
		
		String finalResults =  ee.EvaluatePrefixExpression(modQuery);		
		
		if(finalResults.equals("0"))
			return false;
		else
			return true;	*/	
		return false;
		
	}/* End of checkQuerySatisfied()*/

	public boolean checkTerminationOfSimulation2()
	{
		int count=0;
		
		//int numAgents = agentSample.size();	
		boolean isValid = true;
		
		for(int i=0; i<3; i++)
		{
			int numAgents = nodes.get(i).people.size();
			
			for(int j=0; j<ConfigParameters.num_People; j++)
			{
								
				if((population.get(j).attributes.get(0) == i)&&(population.get(j).attributes.get(1) == 2))
				{
					count = count+1;
				}				
			}	
			
			if(count <= numAgents*0.95)
			{
				isValid = false;
			}				
		}	
		return isValid;
		
	}/*End of checkTerminationOfSimulation()*/
	
	public boolean checkTerminationOfSimulation()
	{
		int count=0;		
		for(int i=0; i<ConfigParameters.num_People; i++)
		{
			if(population.get(i).attributes.get(1) == 1)
				count = count+1;	
		}
		if(count == 0)
			return true;
		else
			return false;
	}/*End of checkTerminationOfSimulation()*/

	public boolean checkTerminationOfSimulation_1()
	{
		
		for(int i=0; i<ConfigParameters.num_People; i++)
		{
			if(population.get(i).attributes.get(1) == 1)
				return false;			
		}
		return true;
	}/*End of checkTerminationOfSimulation()*/

	public void clearAllCityInformation() throws IOException 
	{
		for(int i=0; i<ConfigParameters.num_Nodes; i++)
			nodes.get(i).people.clear();
		/*long startTime = System.currentTimeMillis();
		for(int i=0; i<ConfigParameters.num_Nodes; i++)
			nodes.get(i).people.clear();
		long endTime = System.currentTimeMillis();
		long duration = (endTime - startTime);  //Total execution time in milli seconds
		
		FileWriter fw =  new FileWriter("restruct-time.txt",true);
    	fw.write("\nExecution Time clearAllCityInformation():" + duration);
    	fw.close();*/
		
	}/* End of clearAllCityInformation() */


	public void clearAllAgentNeighborInformation() throws IOException 
	{		
		for(int i=0; i<ConfigParameters.num_People; i++)
			population.get(i).nbrList.clear();
		/*long startTime = System.currentTimeMillis();
		
		for(int i=0; i<ConfigParameters.num_People; i++)
			population.get(i).nbrList.clear();
		
		long endTime = System.currentTimeMillis();
		long duration = (endTime - startTime);  //Total execution time in milli seconds
		
		FileWriter fw =  new FileWriter("restruct-time.txt",true);
    	fw.write("\nExecution Time clearAllAgentNeighborInformation():" + duration);
    	fw.close();*/
		
	}/*End of clearAllAgentNeighborInformation() */


	public void postConfiguration()
	{
		
	}/* End of postConfiguration()*/	
	
	public void parallizeAgentLocationUpdation()
	{
		ExecutorService pool = Executors.newFixedThreadPool(8);  
		int numCity = nodes.size();				 
		
		ParallizeLocationUpdation [] mt = new ParallizeLocationUpdation[numCity];	
		
	    for(int i=0; i<numCity; i++)
	    {
	    	mt[i] = new ParallizeLocationUpdation("thread"+i, nodes.get(i));
	    	
	    }
	    for (int i = 0; i <numCity; i++) 
	    {
	    	pool.execute(mt[i]);
	    }
	    try
	    {
	     Thread.sleep(0);
	    }
	    catch (InterruptedException e) 
	    {
	     // System.out.println("Main thread Interrupted");
	    }	  
	    pool.shutdown(); 	   
 	    while(!pool.isTerminated()) {}
	    
	}/* End of parallizeAgentState() */

		
	public void parallizeAgentStateUpdation()
	{
	    ExecutorService pool = Executors.newFixedThreadPool(numCores);  
	    
	    int numPartitions = partitions.size();				 		
	    ParallizeStateUpdation [] mt = new ParallizeStateUpdation[numPartitions];
	    
	    for(int i=0; i<partitions.size(); i++)
	    {
	    	mt[i] = new ParallizeStateUpdation("thread"+i, partitions.get(i));	    	
	    }
	    for (int i = 0; i <partitions.size(); i++) 
	    {
	    	pool.execute(mt[i]);
	    }	    
	    pool.shutdown(); 	 
	    while (!pool.isTerminated()){}   
	
	}/* End of parallizeAgentState() */
	
	public void nextStateOfAgents() throws IOException
	{

		if(ConfigParameters.compare_Strategy == 1) 
		{
			ModelEpidemic.readNextStateOfAgents(population);			
			parallizeAgentStateUpdation();			
			
			/*long startTime = System.currentTimeMillis();			
			ModelEpidemic.readNextStateOfAgents(population);			
			parallizeAgentStateUpdation();			
			long endTime = System.currentTimeMillis();
			long duration = (endTime - startTime);  //Total execution time in milli seconds			
			FileWriter fw =  new FileWriter("restruct-time.txt",true);
	    	fw.write("\nExecution Time nextStateOfAgents():" + duration);
	    	fw.close();	*/		
		}		
		else
		{			
			parallizeAgentStateUpdation();
		}
	}/* End of nextStateOfAgents() */
	
	public void nextPositionOfAgents() throws IOException 
	{
		if(ConfigParameters.compare_Strategy == 1) 
		{
			ModelEpidemic.readNextPositionsOfAgents(population);
		}		
		else
		{
			for(int i=0; i<ConfigParameters.num_People; i++)
			{
				ModelEpidemic.updationRulesForAgentLocation(population.get(i), Math.random());
			}
		}
	}/*End of nextPositionOfAgents() */	
	
	
	public void parallizeAgentNeighborsListUpdation()
	{
		ExecutorService pool = Executors.newFixedThreadPool(8);   

		int numCity = nodes.size();
	    	
		ParallizeNeighborsListUpdation [] mt = new ParallizeNeighborsListUpdation[numCity];
		
	    for(int i=0; i<numCity; i++)
	    {
	    	mt[i] = new ParallizeNeighborsListUpdation("thread"+i, nodes.get(i));	    	
	    }
	    
	    for(int i=0; i<numCity; i++)
	    {
	    	pool.execute(mt[i]);
	    	
	    }
	    try
	    {
	     Thread.sleep(0);
	    }
	    catch (InterruptedException e) 
	    {
	     // System.out.println("Main thread Interrupted");
	    }
	   pool.shutdown();
	    while(!pool.isTerminated()) {}
	}/* End of parallizeAgentState() */
	
	public void nextNeighborsListOfAgents() throws IOException
	{
		
		if(ConfigParameters.compare_Strategy == 1) 
		{
			ModelEpidemic.readNeighborsListOfAgents(population, nodes);			
			parallizeAgentNeighborsListUpdation();
			
			/*long startTime = System.currentTimeMillis();
			
			ModelEpidemic.readNeighborsListOfAgents(population, nodes);
			
			parallizeAgentNeighborsListUpdation();
			
			long endTime = System.currentTimeMillis();
			long duration = (endTime - startTime);  //Total execution time in milli seconds
			
			FileWriter fw =  new FileWriter("restruct-time.txt",true);
	    		fw.write("\nExecution Time nextNeighborsListOfAgents():" + duration);
	    		fw.close();	*/					
		}		
		else
		{
			for(int i=0; i<ConfigParameters.num_People; i++)
			{
				int loc = population.get(i).attributes.get(0);							
				ModelEpidemic.updationRulesForAgentNeighborsList(population.get(i), nodes.get(loc));
			}
			//parallizeAgentNeighborsListUpdation();
		}
		
	}/*End of nextNeighborsListOfAgents() */
	
		
	public String readAndParseQuery()
	{
		/*String query = ConfigParameters.query;		
		query = query.replaceAll(" ","");
		query = query.toUpperCase();	
		
		QueryProcessing qp = new QueryProcessing();
		
		int numExp = qp.findTotalExpressionsInQuery(query);
		
		queryExpressions = new int[numExp][6];
	    operandPositions   = new int[numExp];
	    
	    return qp.parseQuery(query, queryExpressions, operandPositions);	*/	
		//System.out.println("Final Results: " + op);
	    return "";
	}/* End of readQuery()*/	
}
