<template>
    <div>
        <div>
            <v-row cols="12">
                <v-col cols="6">
                    <div style="display:flex;">
                        <div class="label-title">FeedbackId</div>
                        <String label="입력하세요." v-model="value.feedbackId" :editMode="editMode"/>
                    </div>
                </v-col>
                <v-col cols="6">
                    <div style="display:flex;">
                        <div class="label-title">FeedbackText</div>
                        <String label="입력하세요." v-model="value.feedbackText" :editMode="editMode"/>
                    </div>
                </v-col>
            </v-row>
        </div>

        <div>
            <v-row cols="12">
            </v-row>
        </div>

        <div>
        </div>

        <v-divider class="border-opacity-100 my-divider"></v-divider>
        <v-layout row justify-end>
            <v-btn
                width="64px"
                color="primary"
                @click="save"
            >
                저장
            </v-btn>
        </v-layout>

        <v-snackbar
            v-model="snackbar.status"
            :top="true"
            :timeout="snackbar.timeout"
            color="error"
        >
            {{ snackbar.text }}
            <v-btn dark text @click="snackbar.status = false">
                Close
            </v-btn>
        </v-snackbar>
    </div>
</template>

<script>
const axios = require('axios').default;

import FeedbackBase from '../components/FeedbackBase.vue'

import { RSocketClient } from 'rsocket-core';
import RSocketWebSocketClient from 'rsocket-websocket-client';
import { IdentitySerializer, JsonSerializer } from "rsocket-core/build";



export default {
    name: 'Feedback',
    mixins:[FeedbackBase],
    components:{
    },
    props: {
        value: [Object, String, Number, Boolean, Array],
        editMode: Boolean,
        isNew: Boolean,
        offline: Boolean,
    },
    data: () => ({
        snackbar: {
            status: false,
            timeout: 5000,
            text: ''
        },
    }),
    created(){
        if(this.isNew) return;

        var websocketUrl = new URL(window.location.href);

        websocketUrl.protocol = "wss";
        websocketUrl.pathname = "/rsocket/feedbacks";
        websocketUrl.hash = "";
        
        var me = this;

        const transport = new RSocketWebSocketClient(
            {
                url: websocketUrl.href
            }
        );
        const client = new RSocketClient({
            // send/receive JSON objects instead of strings/buffers
            serializers: {
            data: JsonSerializer,
            metadata: IdentitySerializer
            },
            setup: {
            // ms btw sending keepalive to server
            keepAlive: 60000,
            // ms timeout if no keep-alive response
            lifetime: 180000,
            dataMimeType: "application/json",
            metadataMimeType: 'message/x.rsocket.routing.v0'
            },
            transport
        });
        client.connect().subscribe({
            onComplete: socket => {
            let requestedMsg = 10;

            // console.log("connected to rsocket"); // debug
            const endpoint = "feedbacks."+ me.value.feedbackId +".get"
            socket.requestStream({
                data: {},
                metadata: String.fromCharCode(endpoint.length) + endpoint
            })
                .subscribe({
                    onSubscribe: (sub) => {
                        console.log("subscribed to server stream"); // debug
                        this.requestStreamSubscription = sub
                        this.requestStreamSubscription.request(requestedMsg)
                    },
                    onNext: (e) => {
                        e.data._links = me.value._links;
                        me.value = e.data
                        
                    },
                    onError: error => {
                        // console.log("got error with requestStream"); // debug
                        console.error(error);
                    },
                    onComplete: () => {
                        // console.log("requestStream completed"); // debug
                    }
                });
            },
            onError: error => {
                console.error(error);
            },
            // onSubscribe: cancel => {
            // }
        });

    },
    computed:{
    },
    methods: {
        async init() {
            var me = this;
            let lists = await me.search();
            me.values = lists;
        },
        selectFile(){
            if(this.editMode == false) {
                return false;
            }
            var me = this
            var input = document.createElement("input");
            input.type = "file";
            input.accept = "image/*";
            input.id = "uploadInput";
            
            input.click();
            input.onchange = function (event) {
                var file = event.target.files[0]
                var reader = new FileReader();

                reader.onload = function () {
                    var result = reader.result;
                    me.imageUrl = result;
                    me.value.photo = result;
                    
                };
                reader.readAsDataURL( file );
            };
        },
        edit() {
            this.editMode = true;
        },
        async save(){
            try {
                var temp = null;

                if(!this.offline) {
                    if(this.isNew) {
                        temp = await axios.post(axios.fixUrl('/feedbacks'), this.value)
                    } else {
                        temp = await axios.put(axios.fixUrl(this.value._links.self.href), this.value)
                    }
                }
                if(this.value!=null) {
                    for(var k in temp.data) this.value[k]=temp.data[k];
                } else {
                    this.value = temp.data;
                }

                this.editMode = false;
                this.$emit('input', this.value);

                if (this.isNew) {
                    this.$emit('add', this.value);
                } else {
                    this.$emit('edit', this.value);
                }

                location.reload()

            } catch(e) {
                this.snackbar.status = true
                if(e.response && e.response.data.message) {
                    this.snackbar.text = e.response.data.message
                } else {
                    this.snackbar.text = e
                }
            }
            
        },
        async remove(){
            try {
                if (!this.offline) {
                    await axios.delete(axios.fixUrl(this.value._links.self.href))
                }
                this.editMode = false;
                this.isDeleted = true;

                this.$emit('input', this.value);
                this.$emit('delete', this.value);
            } catch(e) {
                this.snackbar.status = true
                if(e.response && e.response.data.message) {
                    this.snackbar.text = e.response.data.message
                } else {
                    this.snackbar.text = e
                }
            }
        },
        change(){
            this.$emit('input', this.value);
        },
    },
}
</script>

