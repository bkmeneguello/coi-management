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
	
	var PessoaRowView = Marionette.ItemView.extend({
		template: '#pessoa_row_template',
		tagName: 'tr',
		events: {
			'click .coi-action-update': 'doUpdate',
			'click .coi-action-delete': 'doDelete'
		},
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			this.$('button').button();
		},
		doUpdate: function(e) {
			e.preventDefault();
			Backbone.history.navigate('pessoa/' + this.model.get('id'), true);
		},
		doDelete: function(e) {
			e.preventDefault();
			
			var model = this.model;
			_promptDelete(function() {
				model.destroy({
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
	
	var PessoasImportView = Marionette.ItemView.extend({
		template: '#pessoas_import_template',
		initialize: function() {
			_.bindAll(this);
		},
		ui: {
			'file': '#file'
		},
		onRender: function() {
			var that = this;
			this.$el.form();
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
		        }
		    });
			this.$el.dialog({
				title: 'Importar Clientes',
				dialogClass: 'no-close',
				height: 200,
				width: 400,
				modal: true,
				buttons: {
					'Cancelar': that.onCancel,
					'Confirmar': that.onConfirm
				}
			});
		},
		onCancel: function(e) {
			e.preventDefault();
			this.$el.dialog('close');
			this.close();
		},
		onConfirm: function(e) {
			e.preventDefault();
			this.ui.file.fileupload('send', {fileInput: this.ui.file});
		}
	});
	
	var PessoasView = Marionette.CompositeView.extend({
		template: '#pessoas_template',
		tagName: 'form',
		itemViewContainer: 'tbody',
		itemView: PessoaRowView,
		events: {
			'click .coi-action-cancel': 'doCancel',
			'click .coi-action-create': 'doCreate',
			'click .coi-action-import': 'doImport',
			'click .coi-action-search': 'doSearch',
			'click .coi-action-clear': 'doClear',
			'click .coi-action-prev': 'doPrev',
			'click .coi-action-prox': 'doNext'
		},
		ui: {
			'table': 'table',
			'fileupload': '#fileupload',
			'search': '.coi-input-search'
		},
		initialize: function() {
			_.bindAll(this);
			this.collection.fetch();
		},
		onRender: function() {
			this.$el.form();
			this.ui.table.table().css('width', '100%');
		},
		doCancel: function(e) {
			e.preventDefault();
			Backbone.history.navigate('', true);
		},
		doCreate: function(e) {
			e.preventDefault();
			Backbone.history.navigate('pessoa', true);
		},
		doImport: function(e) {
			e.preventDefault();
			new PessoasImportView({collection: this.collection}).render(); //FIXME
		},
		doSearch: function(e) {
			e.preventDefault();
			this.collection.fetch({data: {term: this.ui.search.val()}});
		},
		doClear: function(e) {
			e.preventDefault();
			this.ui.search.val(null);
			this.collection.fetch();
		},
		doPrev: function(e) {
			e.preventDefault();
			this.collection.prevPage({data: {term: this.ui.search.val()}});
		},
		doNext: function(e) {
			e.preventDefault();
			this.collection.nextPage({data: {term: this.ui.search.val()}});
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('pessoas', function() {
			COI.body.show(new PessoasView({collection: new Pessoas()}));
		});
	});
});