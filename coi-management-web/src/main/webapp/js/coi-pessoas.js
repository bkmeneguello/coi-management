"use strict";

COI.module("Pessoas", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Pessoa = Backbone.Model.extend({
		defaults: {
			nome: null,
			codigo: null
		}
	});

	var Pessoas = Backbone.Paginator.requestPager.extend({
		url: '/rest/pessoas',
		model: Pessoa,
		paginator_core: {
			type: 'GET',
			dataType: 'json',
			url: '/rest/pessoas'
		},
		paginator_ui: {
			firstPage: 0,
			currentPage: 0
		},
		server_api: {
			'page' : function() {
				return this.currentPage;
			}
		}
	});
	
	var PessoaRowView = COI.ActionRowView.extend({
		template: '#pessoa_row_template',
		onUpdate: function(e) {
			Backbone.history.navigate('pessoa/' + e.model.get('id'), true);
		},
		onDelete: function(e) {
			_promptDelete(function() {
				e.model.destroy({
					wait: true,
					success: function(model, response, options) {
						_notifyDelete();
					},
					error: function(model, xhr, options) {
						_notifyDeleteFailure();
					}
				});
			});
		}
	});
	
	var PessoasImportView = COI.PopupFormView.extend({
		template: '#pessoas_import_template',
		header: 'Importar Clientes',
		height: 250,
		width: 600,
		initialize: function() {
			_.bindAll(this);
		},
		ui: {
			'file': '#file'
		},
		onRender: function() {
			var that = this;
			this.ui.file.fileupload({
				url: '/rest/pessoas/import',
				autoUpload: false,
				replaceFileInput: false,
				acceptFileTypes: /(\.|\/)(csv)$/i,
		        dataType: 'json',
		        done: function (e, data) {
		        	that.collection.fetch();
		        	that.$el.dialog('close');
					that.close();
		        },
		        progressall: function (e, data) {
		            var progress = parseInt(data.loaded / data.total * 100, 10);
		            $('#progress .bar').css('width', progress + '%');
		            if (progress >= 99) {
		            	$('#progress .bar').remove();
		            	$('#progress')
		            		.append($('<div>', {css: {'width': '100%', 'text-align': 'center'}}).append($('<img>', {src: 'images/loader.gif'})))
		            		.append($('<div>', {text: 'Processando no servidor. Pode demorar, aguarde...'}))
		            		.append($('<div>', {text: 'Esta tela será fechada automaticamente quando estiver concluído.'}));
		            }
		        }
		    });
		},
		onCancel: function(e) {
			this.$el.dialog('close');
			this.close();
		},
		onConfirm: function(e) {
			$('#file').hide();
			this.ui.file.fileupload('send', {fileInput: this.ui.file});
		}
	});
	
	var PessoasView = COI.GridView.extend({
		searchView: COI.SimpleFilterView,
		itemView: PessoaRowView,
		templateHelpers: {
			header: 'Pessoas',
			columns: {
				nome: 'Nome',
				codigo: 'Código'
			}
		},
		extras: {
			'import': {
				text: 'Importar',
				trigger: 'import'
			}
		},
		initialize: function() {
			this.collection.fetch();
		},
		onCancel: function(e) {
			Backbone.history.navigate('', true);
		},
		onCreate: function(e) {
			Backbone.history.navigate('pessoa', true);
		},
		onImport: function(e) {
			new PessoasImportView({collection: this.collection}).render(); //FIXME
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('pessoas', function() {
			COI.body.show(new PessoasView({collection: new Pessoas()}));
		});
	});
});