package com.yacineApp.uniEXMusic.components.utils;

public abstract class AsyncTask<Param, Progress, Result> {

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            done(doInBackground());
        }
    };
    private final Thread currentThread = Thread.currentThread();
    private Thread thread = new Thread(runnable);

    private void done(Result result){
        synchronized (this.currentThread){
            onPostExecute(result);
        }
    }

    protected void onPreExecute(){

    }

    protected void onPostExecute(Result result){

    }

    protected abstract Result doInBackground(Param... params);

    public void execute(){
        onPreExecute();
        thread.start();
    }
}