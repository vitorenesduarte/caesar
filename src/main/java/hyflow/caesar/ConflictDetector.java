package hyflow.caesar;

import hyflow.common.Request;
import hyflow.common.RequestId;
import hyflow.common.RequestStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by balajiarun on 3/11/16.
 */
public class ConflictDetector {

    private final Map<RequestId, Request> requestMap;
    private final List<Request>[] objReqMap;
    private final Logger logger = LogManager.getLogger(ConflictDetector.class);

    public ConflictDetector(int numObjects) {
        requestMap = new HashMap<>(100000);
        objReqMap = new List[numObjects];
        for (int i = 0; i < numObjects; i++) {
            objReqMap[i] = new ArrayList<Request>(100000);
        }
    }

    public void putRequest(Request request) {
        requestMap.put(request.getRequestId(), request);
        for(int oId : request.getObjectIds()) {
            logger.debug("Putting objId " + oId + "from request " + request + " into objReqMap");
            objReqMap[oId].add(request);
        }
    }

    public Request getRequest (RequestId rId) {
        if(requestMap.containsKey(rId)) {
            return requestMap.get(rId);
        }
        return null;
    }

    public Request findWaitRequest(Request request) {
        int[] objectIds = request.getObjectIds();
        for (int oId : objectIds) {
            Request ret = objReqMap[oId].stream().filter((Request r) ->
                    r.getPosition() > request.getPosition() && !r.getPred().contains(request)
                            && r.getStatus() != RequestStatus.Stable
                            && r.getStatus() != RequestStatus.Accepted
            )
                    .findFirst().orElseGet(() -> null);
            if (ret != null) return ret;
        }
        return null;
    }

    public boolean noConflictFor(Request request) {
        int[] objectIds = request.getObjectIds();
        for (int oId : objectIds) {
            boolean conflict = objReqMap[oId].stream().anyMatch((Request r) ->
                r.conflictsWith(request)
            );
            if(conflict) return false;
        }
        return true;
    }

    public long findHighestPosition(Request request) {
        long maxPosition = -1;
        int[] objectIds = request.getObjectIds();
        for (int oId : objectIds) {
            maxPosition = Math.max(
                    objReqMap[oId].stream().max((Request r1, Request r2)->
                            (int) (r1.getPosition() - r2.getPosition())).get().getPosition()
                    , maxPosition);

        }
        assert maxPosition > -1 : "Invalid max position";
        return maxPosition;
    }

    public void updatePred(Request request) {
        for(int oId : request.getObjectIds()) {
            for(Request r: objReqMap[oId]) {
                request.getPred().add(r.getRequestId());
            }
        }
    }
}
