import java.io.*;
import java.net.*;
import org.json.simple.*;
import org.json.simple.parser.*;

class VKsg extends Thread{
    
    public static void main(String[] args) throws Exception{
        
        String tt="📺 @T\n"+
                  "🎬 @S сезон ⟩ @E серия\n"+
                  "🎤 Озвучка ⟩ LostFilm\n"+
                  "💻 Онлайн БЕЗ РЕКЛАМЫ ⟩ @U";
        
        new VKsg("5.87",true,"?","сорвиголова",tt.replace("@T","СОРВИГОЛОВА").replace("@S","3").replace("@E","3").replace("@U","?/dvl/3/"),"?");
        
    }
    
    String API_VERSION;
    boolean IS_USER;
    String ACCESS_TOKEN;
    
    String QUERY;
    String TEXT;
    String ATTACHMENT;
    
    VKsg(String av, boolean iu, String at, String q, String t, String a) throws Exception{
        
        API_VERSION=av;
        IS_USER=iu;
        ACCESS_TOKEN=at;
        
        QUERY=q;
        TEXT=t;
        ATTACHMENT=a;
        
        start();
        
    }
    
    public void run(){
        
        try{
            
            long[] gida=searchGroups(QUERY);
            for(int i=0;i<10 && i<gida.length;i=(i+1)%Math.min(10,gida.length))
                
                try{
                
                    System.out.println(getName()+": Group ["+i+"] https://vk.com/club"+gida[i]+"...");
                    long cpc=canPostComment(gida[i]);
                    String lpit[]=null;
                    
                    if(cpc==1)
                        
                        if((lpit=getLastPost(gida[i]))!=null && lpit[1].equals(TEXT))continue;
                        else{
                            long ec=createPost(gida[i],TEXT,ATTACHMENT);
                            if(ec==0){
                                System.out.println(getName()+":  Post sent!");
                                Thread.sleep(5000);
                            }
                            else{
                                System.out.println(getName()+":  Can't send post (code "+ec+")!");
                                return;
                            }
                        }
                        
                    else if(cpc==2 && (lpit=getLastPost(gida[i]))!=null)
                        
                        if(sameComment(gida[i],Long.valueOf(lpit[0]),3,TEXT))continue;
                        else{
                            long ec=createComment(gida[i],Long.valueOf(lpit[0]),TEXT,ATTACHMENT);
                            if(ec==0){
                                System.out.println(getName()+":  Comment sent!");
                                Thread.sleep(5000);
                            }
                            else{
                                System.out.println(getName()+":  Can't send comment (code "+ec+")!");
                                return;
                            }
                        }
                        
                    else gida=delFromArrayLong(gida,i--);
                    
                }catch(Exception e){
                    e.printStackTrace();
                }
        
        }catch(Exception e){
            e.printStackTrace();
        }
            
    }
    
    String req(String m, String ... p) throws Exception{
            
        String rq="https://api.vk.com/method/"+m+'?';
        
        for(int i=0;i<p.length;i++)
            rq+='&'+p[i];
        
        rq+='&'+"v="+API_VERSION;
        rq+='&'+"access_token="+ACCESS_TOKEN;
            
        URLConnection u = new URL(rq).openConnection();
        u.setConnectTimeout(20000);
        u.setReadTimeout(20000);
            
        try(InputStream is=u.getInputStream()){
                
            rq="";
            int l=-1;
            byte[] ba=new byte[1024];
            while((l=is.read(ba))!=-1)
                rq+=new String(ba,0,l);
                
        }
            
        Thread.sleep(IS_USER?1000/3+5:1000/20+5);
            
        return rq;
        
    }
    
    private long[] searchGroups(String q) throws Exception{
        
        long[] gida=new long[0];
        VKja ja=new VKjo(req("groups.search","q="+URLEncoder.encode(q,"UTF-8"),"count=1000")).o("response").a("items");
        for(int i=0;i<ja.sz();i++)
            gida=addToArrayLong(gida,ja.o(i).l("id"));
            
        return gida;
        
    }
    
    private long canPostComment(long gid) throws Exception{
        
        VKja ja=new VKjo(req("groups.getById","group_id="+gid,"fields=wall")).a("response");
        if(ja.sz()!=1)return -3;
        if(ja.o(0).s("deactivated")!=null)return -2;
        if(ja.o(0).l("is_closed")!=0)return -1;
        else return ja.o(0).l("wall")!=null?ja.o(0).l("wall"):0;
        
    }
    
    private String[] getLastPost(long oid) throws Exception{
        
        VKja ja=new VKjo(req("wall.get","owner_id=-"+oid,"count=2")).o("response").a("items");
        for(int i=0;i<ja.sz();i++)
            if(ja.o(i).l("is_pinned")!=null)continue;
            else return new String[]{ja.o(i).l("id")+"",ja.o(i).s("text")};
            
        return null;
        
    }
    
    private boolean samePost(long oid, long pid, long c, String tt) throws Exception{
        
        VKja ja=new VKjo(req("wall.getComments","owner_id=-"+oid,"post_id="+pid,"count="+c,"sort=desc")).o("response").a("items");
        for(int i=0;i<ja.sz();i++)
            if(ja.o(i).s("text").equals(tt))
                return true;
            
        return false;
        
    }
    
    private boolean sameComment(long oid, long pid, long c, String tt) throws Exception{
        
        VKja ja=new VKjo(req("wall.getComments","owner_id=-"+oid,"post_id="+pid,"count="+c,"sort=desc")).o("response").a("items");
        for(int i=0;i<ja.sz();i++)
            if(ja.o(i).s("text").equals(tt))
                return true;
            
        return false;
        
    }
    
    private long createPost(long oid, String m, String a) throws Exception{
        
        VKjo jo=new VKjo(req("wall.post","owner_id=-"+oid,"message="+URLEncoder.encode(m,"UTF-8"),"attachments="+a));
        
        if(jo.o("error")!=null)return jo.o("error").l("error_code");
        else return 0;
        
    }
    
    private long createComment(long oid, long pid, String m, String a) throws Exception{
        
        VKjo jo=new VKjo(req("wall.createComment","owner_id=-"+oid,"post_id="+pid,"message="+URLEncoder.encode(m,"UTF-8"),"attachments="+a));
        
        if(jo.o("error")!=null)return jo.o("error").l("error_code");
        else return 0;
        
    }
    
    static long[] addToArrayLong(long[] a, long e){
        
        long[] b=new long[a.length+1];
        for(int i=0;i<a.length;i++)
            b[i]=a[i];
        b[a.length]=e;
        
        return b;
        
    }
    
    static long[] delFromArrayLong(long[] a, int e){
        
        long[] b=new long[a.length-1];
        for(int i=0;i<e;i++)
            b[i]=a[i];
        for(int i=e+1;i<a.length;i++)
            b[i-1]=a[i];
        
        return b;
        
    }
    
}

class VKjo{
    
    JSONObject jo;
    
    VKjo(String j) throws Exception{
        
        jo=(JSONObject)new JSONParser().parse(j);
        
    }
    
    VKjo(Object o){
        
        jo=(JSONObject)o;
        
    }
    
    VKjo o(String k){
        
        Object o=jo.get(k);
        return o!=null?new VKjo(o):null;
        
    }
    
    VKja a(String k){
        
        Object o=jo.get(k);
        return o!=null?new VKja(o):null;
        
    }
    
    String s(String k){
        
        Object o=jo.get(k);
        return o!=null?(String)o:null;
        
    }
    
    Long l(String k){
        
        Object o=jo.get(k);
        return o!=null?(Long)o:null;
        
    }
    
    Boolean b(String k){
        
        Object o=jo.get(k);
        return o!=null?(Boolean)o:null;
        
    }
    
}

class VKja{
    
    JSONArray ja;
    
    VKja(String j) throws Exception{
        
        ja=(JSONArray)new JSONParser().parse(j);
        
    }
    
    VKja(Object o){
        
        ja=(JSONArray)o;
        
    }
    
    int sz(){
        
        return ja.size();
        
    }
    
    VKja a(int i){
        
        Object o=ja.get(i);
        return o!=null?new VKja(o):null;
        
    }
    
    VKjo o(int i){
        
        Object o=ja.get(i);
        return o!=null?new VKjo(o):null;
        
    }
    
    String s(int i){
        
        Object o=ja.get(i);
        return o!=null?(String)o:null;
        
    }
    
    Long l(int i){
        
        Object o=ja.get(i);
        return o!=null?(Long)o:null;
        
    }
    
    Boolean b(int i){
        
        Object o=ja.get(i);
        return o!=null?(Boolean)o:null;
        
    }
    
}