package com.android.messaging.smil.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.android.messaging.datamodel.data.MessagePartData;


public class SmilPartEntity extends ArrayList<MessagePartData>{

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;
   public SmilPartEntity()
    {
        super();
         
    }
    
    public void add(int nIndex, MessagePartData ins)
    {
        if( nIndex < 0 || nIndex > size()){
            // out of bounds
            return ;
        }
        else
        {
              super.add(nIndex, ins);
        }
        
    }
   
    
    public boolean addAll(Collection<? extends MessagePartData> collection)
    {
  
         return super.addAll(collection);
       
    }
   
    public MessagePartData get(int nIndex){
        if(nIndex < 0  || nIndex >= size()){
            return null;
        }
        else{
            return super.get(nIndex);
        }
    }
    
    public boolean addAll(int nIndex, Collection<? extends MessagePartData> collection)
    {
        if(nIndex < 0 || nIndex > size()){
            return false;
        }
        else{
         return super.addAll(nIndex, collection);
        }
        
    }
    
    public MessagePartData remove(int nIndex){
        if( nIndex < 0 || nIndex >= size()){
            return null; 
        }
        else
        {
           return  super.remove(nIndex);
        }
        
       
    }
    
    public void removeRange(int nIndexFrom, int toIndex){
        if( nIndexFrom < 0  || nIndexFrom >= size() || toIndex <=0 || toIndex >=size()){
            return ;
        }
        else{
            super.removeRange(nIndexFrom, toIndex);
        }
    }
    
    protected final static String TAG= "SmilPartEntity";
}


/*
class SmilPartEntity {
    
    public SmilPartEntity() {
        if(getMessagePartDatas.size() > 0){
            clearMessagePartDatas();
        }
    }

    private List<MessagePartData> mMessagePartDatas = new ArrayList<MessagePartData>();
 

    public List<MessagePartData> getMessagePartDatas() {
        return mMessagePartDatas;
    }

    public void setMessagePartDatas(List<MessagePartData> messagePartDatas) {
        mMessagePartDatas.clear();
        mMessagePartDatas = null;
        mMessagePartDatas = messagePartDatas;
    }

    
    public void addMessagePartDatas(int i, MessagePartData data) {
        if (i < 0  || i > getMessagePartDatas().size()) {
          //  i = messagePartDatas.size() - 1;
        }
        else{
            getMessagePartDatas().add(i, data);
        }
    }

    public void reMoveMessagePartDatas(int i) {
        if (i < 0) {
            i = 0;
        } else if (i > getMessagePartDatas().size()) {
           // i = messagePartDatas.size() - 1;
        }
        getMessagePartDatas().remove(i);
    }

    public void clearMessagePartDatas() {
        if (getMessagePartDatas() != null) {
            getMessagePartDatas().clear();
        }
    }

   
}
*/
