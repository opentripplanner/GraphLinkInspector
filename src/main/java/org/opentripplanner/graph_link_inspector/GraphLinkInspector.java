package org.opentripplanner.graph_link_inspector;

import com.conveyal.osmlib.OSM;
import com.conveyal.osmlib.Way;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by demory on 11/8/17.
 */
public class GraphLinkInspector {
    public static void main(String[] args) {
        System.out.println("Starting GraphLinkInspector...");

        String osmFile = args[0];
        String graphFile = args[1];
        String outputFile = args[2];

        try {

            System.out.println("Loading OSM from " + osmFile);
            OSM osm = new OSM(null);
            osm.readFromFile(osmFile);

            System.out.println("Loading graph from " + graphFile);
            Graph graph = Graph.load(new File(graphFile), Graph.LoadLevel.FULL);
            System.out.println("Graph loaded");

            Set<String> visitedStops = new HashSet<String>();

            FileWriter fw = new FileWriter(outputFile);
            fw.write("stop_id,stop_lat,stop_lon,way_id\n");
            for(Edge e : graph.getEdges()) {
                if(e instanceof StreetTransitLink) {
                    Vertex fromV = e.getFromVertex();
                    if(fromV instanceof SplitterVertex) {
                        TransitStop ts = (TransitStop) e.getToVertex();
                        if (visitedStops.contains(ts.getStopId().toString())) continue;
                        boolean hasServiceEdge = false;
                        long wayId = 0L;
                        for(Edge incEdge : fromV.getOutgoingStreetEdges()) {
                            StreetEdge stEdge = (StreetEdge) incEdge;

                            Way way = osm.ways.get(stEdge.wayId);
                            if(way != null) {
                                String highway = way.getTag("highway");
                                if(highway != null && highway.equals("service")) {
                                    hasServiceEdge = true;
                                    wayId = stEdge.wayId;
                                    break;
                                }
                            }
                        }

                        if (hasServiceEdge) {
                            fw.write(ts.getStopId() + "," + ts.getCoordinate().y + "," + ts.getCoordinate().x + "," + wayId + "\n");
                            visitedStops.add(ts.getStopId().toString());
                        }

                    }
                }
            }

            fw.close();
            System.out.println("Wrote file to " + outputFile);

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
