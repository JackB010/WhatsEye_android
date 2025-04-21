package com.example.whatseye.dataType

class JavaScriptFunctions {
    companion object {
        val URL_TO_BASE64 = """  ((url)=> {
        return new Promise((resolve, reject) => {
                    fetch(url)
                      .then(response => {
                        if (!response.ok) 
                          throw new Error('HTTP error! Status: ' + response.status);
                        return response.blob();
                      })
                      .then(blob => {
                        const reader = new FileReader();
                        reader.onloadend = () => {
                          const base64Data = reader.result.toString().split(',')[1];
                          resolve(base64Data);
                        };
                        reader.onerror = () => {
                          reject(new Error('Error reading file'));
                        };
                        reader.readAsDataURL(blob);
                      })
                      .catch(error => {
                        reject(error);
                      });
                  });
                })
                        """
    }
}