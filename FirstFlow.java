package debug;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.documentum.fc.client.DfClient;
import com.documentum.fc.client.IDfActivity;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfWorkitem;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfList;
import com.documentum.fc.common.DfLoginInfo;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfList;
import com.documentum.fc.common.IDfLoginInfo;
import com.documentum.mthdservlet.IDmMethod;

public class FirstFlow  implements IDmMethod
{

    protected IDfSessionManager m_sessionMgr = null;
    protected String m_docbase = null;
    protected String m_userName = null;
    protected String m_workitemId = null;
    protected String m_ticket = null;

    private static final String USER_KEY = "user";
    private static final String DOCBASE_KEY = "docbase_name";
    private static final String WORKITEM_KEY_2 = "workitemId";  
    private static final String TICKET_KEY = "ticket";
    private static final String WORKITEM_KEY = "packageId";
    
    private IDfId docIdObj;

   

    public void execute(Map params, OutputStream ostream) throws Exception
    {
        initWorkflowParams(params);
        IDfSessionManager sessionManager = login();
        IDfSession session = null;
        
        try {
            IDfId workitemID = new DfId(m_workitemId);
            session = sessionManager.getSession(m_docbase);
            IDfWorkitem workitem = (IDfWorkitem)session.getObject(workitemID);

            workitem.acquire();
            

                
            IDfCollection pkgColl = null;		
	    			

            pkgColl = workitem.getPackages("");
            
            if (pkgColl != null)
                 {
                  while (pkgColl.next())
                    {
			 			String docId = pkgColl.getString("r_component_id");
                        // System.out.println(docId);
                         ostream.write(docId.getBytes());
                         int docCount = pkgColl.getValueCount("r_component_id");
                             
                         for (int i=0; i <=(docCount-1); i++)
                               {
                                 docIdObj = pkgColl.getRepeatingId("r_component_id", i);
                                 if (docIdObj!=null) 
                                    {
                                      //IDfId sysobjID = new DfId(docIdObj);
                                      IDfSysObject doc = (IDfSysObject)session.getObject(docIdObj);
                                      int j = doc.getInt("test_int") ;
                                      doc.setInt("test_int", j++ ) ;
                                      doc.save() ;
                                      
                                     }
                                }        
                     }
                     pkgColl.close();
                   }		
            String objectName = workitem.getActivity().getObjectName() ;
			session.getAuditTrailManager().createAudit(docIdObj,  "custom event", new String [] {" current activity " + objectName}  , new IDfId [] { DfId.DF_NULLID } ) ;
			
			IDfList list = workitem.getForwardActivities();
			if (list != null && list.get(0) != null) {
				IDfActivity act = (IDfActivity) list.get(0);
				
				IDfList out = new DfList();
				out.append(act);
				workitem.setOutput(out) ;
			}			
            workitem.complete();
        } 
        catch (DfException e)
        {
                ostream.write(e.getMessage().getBytes());
                e.printStackTrace();    // spit out to stderr as well
                throw e;
        } finally
        {
            if ( session != null )
                sessionManager.release(session);
        }

    }

    protected void initWorkflowParams(Map params)
    {
        // get the 4 WF-related parameters always passed in by Server
       Set keys = params.keySet();
       Iterator iter = keys.iterator();
       while (iter.hasNext())
       {
           String key = (String) iter.next();
           if( (key == null) || (key.length() == 0) )
           {
               continue;
           }
           String []value = (String[])params.get(key);

           if ( key.equalsIgnoreCase(USER_KEY) )
               m_userName = (value.length > 0) ? value[0] : "";
           else if ( key.equalsIgnoreCase(DOCBASE_KEY) )
               m_docbase = (value.length > 0) ? value[0] : "";
           else if ( key.equalsIgnoreCase(WORKITEM_KEY_2 ) )
               m_workitemId = (value.length > 0) ? value[0] : "";
           else if ( key.equalsIgnoreCase(WORKITEM_KEY ) )
               m_workitemId = (value.length > 0) ? value[0] : "";
           else if ( key.equalsIgnoreCase(TICKET_KEY) )
               m_ticket = (value.length > 0) ? value[0] : "";
       }
   }

   protected IDfSessionManager login() throws DfException
   {
       if (m_docbase == null || m_userName == null || m_ticket == null )
           return null;
           
       // now login
       IDfClient dfClient = DfClient.getLocalClient();

       if (dfClient != null)
       {
           IDfLoginInfo li = new DfLoginInfo();
           li.setUser(m_userName);
           li.setPassword(m_ticket);
           li.setDomain(null);

           IDfSessionManager sessionMgr = dfClient.newSessionManager();
           sessionMgr.setIdentity(m_docbase, li);
           return sessionMgr;
       }

       return null;
   }

}
