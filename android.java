RequestQueue queue = Volley.newRequestQueue(this);
String url = "http://your-server.com/data.php?rfid=1234&gps=12.345,67.890";

StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
        response -> Toast.makeText(this, "Data Sent!", Toast.LENGTH_SHORT).show(),
        error -> Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show());

queue.add(stringRequest);

