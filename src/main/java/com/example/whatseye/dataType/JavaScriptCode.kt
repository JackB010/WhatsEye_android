package com.example.whatseye.dataType

class JavaScriptCode   {
    companion object {

        val CONTACT = """(async ()=>{
           ng = document.querySelector('[data-icon="new-chat-outline"]')
           ng.click()
           await new Promise(resolve => setTimeout(resolve, 10));
           const contactContainer = document.querySelector("div[data-tab='4']");
           const parentElement = contactContainer.parentElement;    
        
           if (contactContainer.querySelectorAll('[role="listitem"]').length === 0) return []
           contactContainer.querySelectorAll('[role="listitem"]').forEach(item=>{ 
              if(item.querySelector("span[title]"))
                 item.setAttribute('aria-selected_0',"false") 
           })
           
           const arrowDownEvent = new KeyboardEvent('keydown', {
                    key: 'ArrowDown',
                    code: 'ArrowDown',
                    keyCode: 40,
                    which: 40,
                    bubbles: true
                });
           const searchBox = document.querySelector('[role="textbox"]');
           searchBox.focus();
           searchBox.dispatchEvent(arrowDownEvent);
           
           nb = contactContainer.firstChild.firstChild.style.height.replace('px', '')/72
           contacts = []
        
           for(let i=0; i<nb; i++){
              await new Promise(resolve => setTimeout(resolve, 120));
              item = contactContainer.querySelector('[aria-selected_0="false"]')
              if(item.getAttribute('DONE') == null){
                 item.setAttribute('DONE', 'true')
                 let name = item.querySelector("span[title]").innerText;   
                 let about = item.querySelector("div[role='gridcell']").parentElement.childNodes[1].firstChild.innerText
                 let icon = item.querySelector('img') ? item.querySelector('img').src : "";
                 contacts.push({ name, about, icon });
                 //contacts.push({ name});
              }
              item.dispatchEvent(arrowDownEvent);
              item.removeAttribute('aria-selected_0')
        
              if(contactContainer.querySelectorAll('[aria-selected_0="false"]').length ===0){
                 if( contactContainer.querySelectorAll('[role="listitem"]').length !== 0){
                     contactContainer.querySelectorAll('[role="listitem"]').forEach(item=>{ 
                    if(item.querySelector("span[title]"))
                       item.setAttribute('aria-selected_0',"false") 
                    })
                 }else{
                    return contacts 
                 }
              }
           }
              bc = document.querySelector('[data-icon="back"]')
              bc.click()
              localStorage.setItem("CONTACT", JSON.stringify(contacts));
            })();""".trimIndent()
        val CURRENT_CHATS = """(async() =>{
               if (document.querySelectorAll('[role="listitem"]').length === 0)
                  return []
               const arrowDownEvent = new KeyboardEvent('keydown', {
                        key: 'ArrowDown',
                        code: 'ArrowDown',
                        keyCode: 40,
                        which: 40,
                        bubbles: true
                    });
               const searchBox = document.querySelector('[role="textbox"]');
               searchBox.focus();
               searchBox.dispatchEvent(arrowDownEvent);
               
               const nb = document.querySelector('[aria-rowcount]').getAttribute('aria-rowcount') 
               contacts = []
               for(let i=0; i<nb; i++){
                  await new Promise(resolve => setTimeout(resolve, 60));
                  item = document.querySelectorAll('[aria-selected="true"]')[1]
                  let name = item.querySelector("span[title]");
                  if (name) {
                     name = name.innerText;
                     const timestamp = item.querySelector("div[role='gridcell']").lastChild.innerText;
                     let last_msg = item.querySelector("div[role='gridcell']").parentElement.childNodes[1].querySelector('[title]').innerText
                     let num_unread = item.querySelector('[role="gridcell"][aria-colindex="1"]').firstChild.innerText
                     let icon = item.querySelector('img') ? item.querySelector('img').src : "";
                       contacts.push({ name, timestamp,num_unread ,last_msg, icon });
                     }
                        document.querySelectorAll('[aria-selected="true"]')[1].dispatchEvent(arrowDownEvent);
                  }
                  
                  const lang = document.querySelector('html').getAttribute('lang')
                  if(item.querySelector('[aria-label="Unread"]') || item.querySelector('[aria-label="غير مقروءة"]') || item.querySelector('[aria-label="Non lues"]') ){
                    await new Promise(resolve => setTimeout(resolve,    300));
                    item.querySelector('button').click()
                    await new Promise(resolve => setTimeout(resolve,    300));
                    
                    if (lang=='en'){
                       action =  document.querySelector('[aria-label="Mark as unread"]')
                       if (action)
                            action.click()
                       }
                    if (lang=='ar'){
                         action =document.querySelector('[aria-label="تمييز كغير مقروءة"]')
                         if (action)
                            action.click()
                        }
                    if (lang=='fr'){
                         action =document.querySelector('[aria-label="Marquer comme non lue"]')
                         if (action)
                            action.click()
                        }
                }   
                 await new Promise(resolve => setTimeout(resolve,    300));
                document.querySelectorAll('[data-icon="menu"]')[1].click()
                await new Promise(resolve => setTimeout(resolve,    300));
                if (lang=='en'){
                    action = document.querySelector('[aria-label="Close chat"]')
                    if (action)
                        action.click()
                    }
                 if (lang=='ar'){
                    action = document.querySelector('[aria-label="إغلاق الدردشة"]')
                    if (action)
                        action.click()
                    }
                  if (lang=='fr'){
                    action = document.querySelector('[aria-label="Fermer la discussion"]')
                    if (action)
                        action.click()
                    }
                    
                await new Promise(resolve => setTimeout(resolve,    300));
                document.getElementById("pane-side").scrollTop = 0;
                
               localStorage.setItem("CURRENT_CHATS", JSON.stringify(contacts))               
            })();""".trimIndent()
        val BLOCK_USER = """(async (search, pos=0)=>{
               if (document.querySelectorAll('[role="listitem"]').length === 0) return []
               const arrowDownEvent = new KeyboardEvent('keydown', {
                        key: 'ArrowDown',
                        code: 'ArrowDown',
                        keyCode: 40,
                        which: 40,
                        bubbles: true
                    });
               const searchBox = document.querySelector('[role="textbox"]');
               searchBox.focus();
               searchBox.dispatchEvent(arrowDownEvent);
               const nb = document.querySelector('[aria-rowcount]').getAttribute('aria-rowcount') 
               let item_pos=0;
               for(let i=0; i<nb; i++){
                  await new Promise(resolve => setTimeout(resolve, 60));
                  item = document.querySelectorAll('[aria-selected="true"]')[1]
                  let name = item.querySelector("span[title]");
                  if (name ) {
                     name = name.innerText;
                     if (search === name){
                        if (item_pos===pos ) {
                            await new Promise(resolve => setTimeout(resolve, 300));
                            item.querySelector('button').click()
                            await new Promise(resolve => setTimeout(resolve, 300));
                            const lang = document.querySelector('html').getAttribute('lang')
                            if (lang=='en')
                                action = document.querySelector('[aria-label="Block"]')
                            if (lang=='fr')
                                action = document.querySelector('[aria-label="Bloquer"]')
                            if (lang=='ar')
                                action = document.querySelector('[aria-label="حظر"]')
                            
                            let block = true

                            if(!action){
                                if (lang=='en')
                                    action = document.querySelector('[aria-label="Exit group"]')
                                if (lang=='fr')
                                    action = document.querySelector('[aria-label="Quitter le groupe"]')
                                if (lang=='ar')
                                    action = document.querySelector('[aria-label="الخروج من المجموعة"]')
                                    
                                block = false
                            }
                            if(action){
                                action.click()
                                await new Promise(resolve => setTimeout(resolve, 300));
                                action = document.querySelector('[role="dialog"]').querySelectorAll('button')
                                action[action.length -1].click()
                            }

                            if(block){
                                await new Promise(resolve => setTimeout(resolve, 300));
                                item.querySelector('button').click()
                                await new Promise(resolve => setTimeout(resolve, 300));
                                if (lang=='en')
                                    document.querySelector('[aria-label="Delete chat"]').click()
                                if (lang=='fr')
                                    document.querySelector('[aria-label="Supprimer la discussion"]').click()
                                if (lang=='ar')
                                    document.querySelector('[aria-label="حذف الدردشة"]').click()
                                await new Promise(resolve => setTimeout(resolve, 300));
                                action = document.querySelector('[role="dialog"]').querySelectorAll('button')
                                action[action.length -1].click()
                         }
                            return;
                        }else {item_pos +=1} 
                        }
                     }
                  document.querySelectorAll('[aria-selected="true"]')[1].dispatchEvent(arrowDownEvent);
                  }
            })""".trimIndent()



        val SELECT_ROOM = """(async (search, pos=0)=>{
               if (document.querySelectorAll('[role="listitem"]').length === 0) return []
               
               const arrowDownEvent = new KeyboardEvent('keydown', {
                        key: 'ArrowDown',
                        code: 'ArrowDown',
                        keyCode: 40,
                        which: 40,
                        bubbles: true
                    });
               const searchBox = document.querySelector('[role="textbox"]');
               searchBox.focus();
               searchBox.dispatchEvent(arrowDownEvent);
               const nb = document.querySelector('[aria-rowcount]').getAttribute('aria-rowcount') 
               let item_pos=0;
               for(let i=0; i<nb; i++){
                  await new Promise(resolve => setTimeout(resolve, 60));
                  item = document.querySelectorAll('[aria-selected="true"]')[1]
                  let name = item.querySelector("span[title]");
                  if (name ) {
                     name = name.innerText;
                     if (search === name){
                     
                        if (item_pos===pos ) {
                        await new Promise(resolve => setTimeout(resolve, 600));       
                        if(item.querySelector('[aria-label="Unread"]') || item.querySelector('[aria-label="غير مقروءة"]') || item.querySelector('[aria-label="Non lues"]') ){
                            await new Promise(resolve => setTimeout(resolve,    300));
                            item.querySelector('button').click()
                            await new Promise(resolve => setTimeout(resolve,    300));
                            const lang = document.querySelector('html').getAttribute('lang')
                            
                            if (lang=='en'){
                               action =  document.querySelector('[aria-label="Mark as unread"]')
                               if (action)
                                    action.click()
                               }
                            if (lang=='ar'){
                                 action =document.querySelector('[aria-label="تمييز كغير مقروءة"]')
                                 if (action)
                                    action.click()
                                }
                            if (lang=='fr'){
                                 action =document.querySelector('[aria-label="Marquer comme non lue"]')
                                 if (action)
                                    action.click()
                                }
                        }   
                        await new Promise(resolve => setTimeout(resolve, 300));
                        document.getElementById("pane-side").scrollTop = 0;
                        await new Promise(resolve => setTimeout(resolve,    300));
                        await localStorage.setItem("ROOM_SELECTED", true);
                        await new Promise(resolve => setTimeout(resolve,    300));
                            return;
                        }else {item_pos +=1} 
                        }
                     }
                  document.querySelectorAll('[aria-selected="true"]')[1].dispatchEvent(arrowDownEvent);
                  }
                
            })""".trimIndent()

        val CHAT_ROOM_SCROLLER2 = """(async () => {
            await new Promise(resolve => setTimeout(resolve,    600));
                const root = document.querySelector('[id="main"]');
                const room = root.querySelector('[role="application"]');
                let prev_scroll = 0;
                let timedOut = false;
               
                // Set timeout to mark as timed out after 30 seconds
            
                 
                 
               do {
               numrows = room.querySelectorAll('div[role="row"]').length 
               
                reload_a= document.querySelector('[title="Load earlier messages…"]')
                if(reload_a){
                await new Promise(resolve => setTimeout(resolve,    300));
                    reload_a.click()
                    await new Promise(resolve => setTimeout(resolve,    300));
                    reload_a.click()
                    }
                    prev_scroll = room.parentElement.scrollTop;
                    await new Promise(resolve => setTimeout(resolve, 5));
                    room.parentElement.scrollTop -= 80;
                    await new Promise(resolve => setTimeout(resolve, 15));
                    room.parentElement.scrollTop += 10;
                    await new Promise(resolve => setTimeout(resolve, 15));
                    messageRows = room.querySelectorAll('div[role="row"]').length
                }  while (prev_scroll !== room.parentElement.scrollTop)
            
                // Cleanup the timeout and set localStorage
                await new Promise(resolve => setTimeout(resolve,    100));
                localStorage.setItem("DONE_LOADING_CHAT2", true);
            })();
        """.trimIndent()

        val CHAT_ROOM_SCROLLER = """(async () => {
    const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

    const root = document.querySelector('[id="main"]');
    const room = root?.querySelector('[role="application"]');

    if (!room) {
        console.error("Chat room not found.");
        return;
    }
          room.parentElement.click()
            await delay(200);
        
            let counted = 1;
            let messageRows = room.querySelectorAll('div[role="row"]');
        
            const arrowUpEvent = new KeyboardEvent('keydown', {
                key: 'ArrowUp',
                code: 'ArrowUp',
                keyCode: 38,
                which: 38,
                bubbles: true
            });
       
            while (counted <= messageRows.length) {
                const loadEarlierButton = document.querySelector('[title="Load earlier messages…"]');
                if (loadEarlierButton) {
                    await delay(200);
                    loadEarlierButton.click();
                    await delay(200);
                    loadEarlierButton.click(); // Maybe redundant — double-clicking
                }
        
                messageRows = room.querySelectorAll('div[role="row"]');
                const targetRow = messageRows[messageRows.length - counted];
        
                if (targetRow?.firstChild) {
                    await delay(30);
                    targetRow.firstChild.dispatchEvent(arrowUpEvent);
                    await delay(30);
                }
                 console.log(counted +"/ "+messageRows.length+"");
                counted++;
            }
            await delay(300);
            localStorage.setItem("DONE_LOADING_CHAT", true);
  
            console.log("Finished loading all messages.");
        })();
""".trimIndent()
        val GET_CHAT = """(async()=> {
                        await new Promise(resolve => setTimeout(resolve, 1000));
                        const root = document.querySelector('[id="main"]');
                        const room = root.querySelector('[role="application"]');
                        const messageList = []; 
                        const messageRows = room.querySelectorAll('div[role="row"]'); // Select all rows containing messages.
                    
                        await new Promise(resolve => setTimeout(resolve, 1000));
                        localStorage.setItem("CHATS", messageRows.length);
                        //return JSON.stringify(messageList, null, 2);
                    })();""".trimIndent()


    }
}