const http=require('http');
const port=3000;
const fs=require('fs');

const server=http.createServer(function(req,res){
    res.writeHead(200,{'Content-Type':'text/html'});
    fs.readFile('index.html');
    res.write('Hello Node');
    res.end();
});

server.listen(port,function(error){
    if(error) console.log('somethong went wrong',error);
    else{
        console.log('server is listening on port '+port+'...')
    }
});
