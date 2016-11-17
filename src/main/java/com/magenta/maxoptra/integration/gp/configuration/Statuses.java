package com.magenta.maxoptra.integration.gp.configuration;

import java.util.ArrayList;
import java.util.List;

public class Statuses {

    public List<Status> status = new ArrayList<>();

    public static class Status {

        public String statusGp;

        public MxStatuses mxStatuses = new MxStatuses();

        public static class MxStatuses {
            public List<String> statusMx = new ArrayList<>();
        }


    }
}
